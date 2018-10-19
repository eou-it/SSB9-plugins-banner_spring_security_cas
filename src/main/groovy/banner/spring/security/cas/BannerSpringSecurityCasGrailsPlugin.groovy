package banner.spring.security.cas

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.web.GrailsSecurityFilterChain
import grails.plugins.Plugin
import grails.util.Holders
import groovy.util.logging.Slf4j
import net.hedtech.banner.cas.validator.BannerCasValidatorFilter
import net.hedtech.banner.controllers.ControllerUtils
import net.hedtech.banner.security.BannerCasAuthenticationFailureHandler
import net.hedtech.banner.security.CasAuthenticationProvider
import org.jasig.cas.client.util.HttpServletRequestWrapperFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.security.cas.web.CasAuthenticationFilter

import javax.servlet.Filter

@Slf4j
class BannerSpringSecurityCasGrailsPlugin extends Plugin {
    String groupId = "net.hedtech"

    String version = '9.28'
    def dependsOn = [
            bannerCore: '9.28.1 => *',
            bannerGeneralUtility:'9.28.1 => *',
            springSecurityCas:'3.1.0'
    ]

    // List loadAfter = ['bannerCore','springSecurityCas']

    List loadAfter = ['bannerCore','bannerGeneralUtility','springSecuritySaml','springSecurityCas']
    def grailsVersion = "3.3.2 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Banner Spring Security Cas" // Headline display name of the plugin
    def author = "Your name"
    def authorEmail = ""
    def description = '''\
Brief summary/description of the plugin.
'''
    def profiles = ['web']

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/banner-spring-security-cas"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
//    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]



    Closure doWithSpring() { {->
        println "--------- In Banner CAS doWithSpring ----------------"
        // TODO Implement runtime spring config (optional)
        def conf = SpringSecurityUtils.securityConfig
        def application = grailsApplication
        def config = application.config
        println "********************************** In banner cas conf ********************************************"

        if(Holders.config.banner?.sso?.authenticationProvider == 'default' || (Holders.config.banner?.sso?.authenticationProvider == 'saml') || (Holders.config.banner?.sso?.authenticationProvider == 'cas' && !conf.cas.active )){
            return
        }
        println '\nConfiguring Banner Spring Security CAS ...'

        casAuthenticationProvider(CasAuthenticationProvider) {
            dataSource = ref(dataSource)
        }

        bannerCasAuthenticationFailureHandler(BannerCasAuthenticationFailureHandler){
            defaultFailureUrl = Holders.config.banner?.sso?.grails?.plugin?.springsecurity?.failureHandler.defaultFailureUrl
        }

        casAuthenticationFilter(CasAuthenticationFilter){
            authenticationManager = ref('authenticationManager')
            sessionAuthenticationStrategy = ref('sessionAuthenticationStrategy')
            authenticationSuccessHandler = ref('authenticationSuccessHandler')
            authenticationFailureHandler = ref('bannerCasAuthenticationFailureHandler')
            rememberMeServices = ref('rememberMeServices')
            authenticationDetailsSource = ref('authenticationDetailsSource')
            serviceProperties = ref('casServiceProperties')
            proxyGrantingTicketStorage = ref('casProxyGrantingTicketStorage')
            filterProcessesUrl = conf.cas.filterProcessesUrl // '/j_spring_cas_security_check'
            continueChainBeforeSuccessfulAuthentication = conf.apf.continueChainBeforeSuccessfulAuthentication // false
            allowSessionCreation = conf.apf.allowSessionCreation // true
            proxyReceptorUrl = conf.cas.proxyReceptorUrl
        }

        bannerCasValidatorFilter(BannerCasValidatorFilter)

        bannerCasValidatorFilterRegistrationBean(FilterRegistrationBean) {
            name = 'CAS Validation Filter'
            filter = ref('bannerCasValidatorFilter')
            urlPatterns = ['/*']
            initParameters = ["casServerUrlPrefix":conf.cas.serverUrlPrefix, "serverName":conf.cas.serverName, "renew":false,"artifactParameterName":conf.cas.artifactParameter, "redirectAfterValidation":true,"ignoreInitConfiguration":true, "tolerance":75000]
        }

        httpServletRequestWrapperFilter(HttpServletRequestWrapperFilter){
        }

        httpServletRequestWrapperFilterRegistrationBean(FilterRegistrationBean){
            name = 'CAS HttpServletRequest Wrapper Filter'
            filter = ref('httpServletRequestWrapperFilter')
            urlPatterns = ['/*']

        }

        println '... finished configuring Banner Spring Security CAS\n'
    }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    void doWithApplicationContext() {
        def conf = SpringSecurityUtils.securityConfig
        println "--------- In Banner CAS doWithApplicationContext ----------------"
        // TODO Implement post initialization spring config (optional)
        // build providers list here to give dependent plugins a chance to register some
        println "********************************** In Banner CAS conf ********************************************"
        println "*****************************************  **********************************************************"
        println "--------- In Banner CAS doWithApplicationContext End ---------------- \n"
        if(Holders.config.banner?.sso?.authenticationProvider == 'default' || (Holders.config.banner?.sso?.authenticationProvider == 'saml') || (Holders.config.banner?.sso?.authenticationProvider == 'cas' && !conf.cas.active )){
            return
        }
        def providerNames = []


        if (conf.providerNames) {
            providerNames.addAll conf.providerNames
        } else {
            //TODO After adding Banner_Core Dependency uncomment below lines.
            if(ControllerUtils.isGuestAuthenticationEnabled()){
                providerNames = ['casAuthenticationProvider','selfServiceBannerAuthenticationProvider','bannerAuthenticationProvider']
            } else{
                providerNames = ['casAuthenticationProvider']
            }
        }
        applicationContext.authenticationManager.providers = createBeanList(providerNames, applicationContext)
        println "applicationContext.authenticationManager.providers In CAS === " +applicationContext.authenticationManager.providers

        // Define the spring security filters


        def authenticationProvider = Holders.config.banner.sso.authenticationProvider
        List<Map<String, ?>> filterChains = []
        println "AuthenticationProvider === " +authenticationProvider
        switch (authenticationProvider) {
            case 'cas':
                filterChains << [pattern: '/**/api/**',   filters: 'statelessSecurityContextPersistenceFilter,bannerMepCodeFilter,authenticationProcessingFilter,basicAuthenticationFilter,securityContextHolderAwareRequestFilter,anonymousProcessingFilter,basicExceptionTranslationFilter,filterInvocationInterceptor']
                filterChains << [pattern: '/**/qapi/**',  filters: 'statelessSecurityContextPersistenceFilter,bannerMepCodeFilter,authenticationProcessingFilter,basicAuthenticationFilter,securityContextHolderAwareRequestFilter,anonymousProcessingFilter,basicExceptionTranslationFilter,filterInvocationInterceptor']
                filterChains << [pattern: '/**',          filters: 'securityContextPersistenceFilter,logoutFilter,bannerMepCodeFilter,bannerCasValidatorFilter,casAuthenticationFilter,authenticationProcessingFilter,securityContextHolderAwareRequestFilter,anonymousProcessingFilter,exceptionTranslationFilter,filterInvocationInterceptor']
                break
            default:
                break
        }

        List<GrailsSecurityFilterChain> chains = new ArrayList<GrailsSecurityFilterChain>()
        for (Map<String, ?> entry in filterChains) {
            String value = (entry.filters ?: '').toString().trim()
            List<Filter> filters = value.toString().split(',').collect { String name -> applicationContext.getBean(name, Filter) }
            chains << new GrailsSecurityFilterChain(entry.pattern as String, filters)
        }
        println ("\n ************* IN CAS *************************\n")

        applicationContext.springSecurityFilterChain.filterChains = chains
        def filterChain = applicationContext.getBean('springSecurityFilterChain')
        println ("filterChain  ===== ${filterChain }")

    }

    private def isSsbEnabled() {
        Holders.config.ssbEnabled instanceof Boolean ? Holders.config.ssbEnabled : false
    }

    private createBeanList(names, ctx) { names.collect { name -> ctx.getBean(name) } }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
