/* *****************************************************************************
 Copyright 2015-2018 Ellucian Company L.P. and its affiliates.
 ****************************************************************************** */
package banner.spring.security.cas

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.web.GrailsSecurityFilterChain
import grails.plugins.Plugin
import grails.util.Holders
import groovy.util.logging.Slf4j
import net.hedtech.banner.controllers.ControllerUtils
import net.hedtech.banner.general.audit.LoginAuditService
import net.hedtech.banner.security.CasAuthenticationProvider
import net.hedtech.jasig.cas.client.BannerSaml11ValidationFilter
import org.jasig.cas.client.session.SingleSignOutFilter
import org.jasig.cas.client.session.SingleSignOutHttpSessionListener
import org.jasig.cas.client.util.HttpServletRequestWrapperFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import net.hedtech.banner.security.BannerCasAuthenticationFailureHandler
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean
import org.springframework.core.Ordered
import org.springframework.security.cas.web.CasAuthenticationFilter

import javax.servlet.Filter

@Slf4j
class BannerSpringSecurityCasGrailsPlugin extends Plugin {
    String groupId = "net.hedtech"

    def dependsOn = [
            bannerCore: '9.30 => *',
            bannerGeneralUtility:'9.30 => *',
            springSecurityCas:'3.1.0'
    ]

    List loadAfter = ['bannerCore','bannerGeneralUtility','springSecurityCas']
    def grailsVersion = "3.3.2 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Banner Spring Security Cas" // Headline display name of the plugin
    def description = '''\
Brief summary/description of the plugin.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/banner-spring-security-cas"

    Closure doWithSpring() { {->
        def conf = SpringSecurityUtils.securityConfig
        if(Holders.config.banner?.sso?.authenticationProvider == 'default' || (Holders.config.banner?.sso?.authenticationProvider == 'saml') || (Holders.config.banner?.sso?.authenticationProvider == 'cas' && !conf.cas.active )){
            return
        }
        println '\nConfiguring Banner Spring Security CAS ...'

        loginAuditService(LoginAuditService)

        casBannerAuthenticationProvider(CasAuthenticationProvider) {
            dataSource = ref(dataSource)
            loginAuditService = ref(loginAuditService)
        }

        bannerCasAuthenticationFailureHandler(BannerCasAuthenticationFailureHandler){
            defaultFailureUrl = Holders.config.grails?.plugin?.springsecurity?.failureHandler?.defaultFailureUrl
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
            filterProcessesUrl = conf.cas.filterProcessesUrl // '/cas/login'
            continueChainBeforeSuccessfulAuthentication = conf.apf.continueChainBeforeSuccessfulAuthentication // false
            allowSessionCreation = conf.apf.allowSessionCreation // true
            proxyReceptorUrl = conf.cas.proxyReceptorUrl
        }


        httpServletRequestWrapperFilter(HttpServletRequestWrapperFilter)
        httpServletRequestWrapperFilterRegistrationBean(FilterRegistrationBean){
            name = 'CAS HttpServletRequest Wrapper Filter'
            filter = ref('httpServletRequestWrapperFilter')
            urlPatterns = ['/*']

        }

        bannerSaml11ValidationFilter(BannerSaml11ValidationFilter){
        }

        bannerSaml11ValidationFilterRegistrationBean(FilterRegistrationBean) {
            name = 'CAS Validation Filter'
            filter = ref('bannerSaml11ValidationFilter')
            urlPatterns = ['/*']
            initParameters = ["casServerUrlPrefix":conf.cas.serverUrlPrefix, "serverName":conf.cas.serverName]
        }

        singleSignOutFilter(SingleSignOutFilter) {
            ignoreInitConfiguration = true
            casServerUrlPrefix = conf.cas.serverUrlPrefix
            artifactParameterName = conf.cas.artifactParameter
        }

        singleSignOutFilterRegistrationBean(FilterRegistrationBean) {
            name = 'CAS Single Sign Out Filter'
            filter = ref('singleSignOutFilter')
            order = Ordered.HIGHEST_PRECEDENCE
        }
        singleSignOutHttpSessionListener(ServletListenerRegistrationBean, new SingleSignOutHttpSessionListener())

        println '... finished configuring Banner Spring Security CAS\n'
    }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    void doWithApplicationContext() {
        def conf = SpringSecurityUtils.securityConfig
        if(Holders.config.banner?.sso?.authenticationProvider == 'default' || (Holders.config.banner?.sso?.authenticationProvider == 'saml') || (Holders.config.banner?.sso?.authenticationProvider == 'cas' && !conf.cas.active )){
            return
        }
        def providerNames = []
        if (conf.providerNames) {
            providerNames.addAll conf.providerNames
        } else {
            if(ControllerUtils.isGuestAuthenticationEnabled()){
                providerNames = ['casBannerAuthenticationProvider','selfServiceBannerAuthenticationProvider','bannerAuthenticationProvider']
            } else{
                providerNames = ['casBannerAuthenticationProvider']
            }
        }
        applicationContext.authenticationManager.providers = createBeanList(providerNames, applicationContext)
        // Define the spring security filters
        def authenticationProvider = Holders.config.banner.sso.authenticationProvider
        List<Map<String, ?>> filterChains = []

        switch (authenticationProvider) {
            case 'cas':
                filterChains << [pattern: '/**/api/**',   filters: 'statelessSecurityContextPersistenceFilter,bannerMepCodeFilter,bannerSaml11ValidationFilter,authenticationProcessingFilter,basicAuthenticationFilter,securityContextHolderAwareRequestFilter,anonymousProcessingFilter,basicExceptionTranslationFilter,filterInvocationInterceptor']
                filterChains << [pattern: '/**/qapi/**',  filters: 'statelessSecurityContextPersistenceFilter,bannerMepCodeFilter,bannerSaml11ValidationFilter,authenticationProcessingFilter,basicAuthenticationFilter,securityContextHolderAwareRequestFilter,anonymousProcessingFilter,basicExceptionTranslationFilter,filterInvocationInterceptor']
                filterChains << [pattern: '/**',          filters: 'securityContextPersistenceFilter,logoutFilter,bannerMepCodeFilter,bannerSaml11ValidationFilter,casAuthenticationFilter,authenticationProcessingFilter,securityContextHolderAwareRequestFilter,anonymousProcessingFilter,exceptionTranslationFilter,filterInvocationInterceptor']
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
        applicationContext.springSecurityFilterChain.filterChains = chains
    }

    private createBeanList(names, ctx) { names.collect { name -> ctx.getBean(name) } }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
