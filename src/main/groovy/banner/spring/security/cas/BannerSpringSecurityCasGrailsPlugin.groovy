package banner.spring.security.cas

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugins.Plugin
import grails.util.Holders
import net.hedtech.banner.controllers.ControllerUtils
import org.springframework.security.web.DefaultSecurityFilterChain
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher

import net.hedtech.banner.security.CasAuthenticationProvider
import net.hedtech.banner.security.BannerCasAuthenticationFailureHandler
import org.springframework.security.cas.web.CasAuthenticationFilter

import javax.servlet.Filter

class BannerSpringSecurityCasGrailsPlugin extends Plugin {
    String groupId = "net.hedtech"

    String version = '9.28'
    def dependsOn = [
            bannerCore: '9.28.1 => *',
            springSecurityCas:'3.1.0'
    ]
    // the version or versions of Grails the plugin is designed for
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

//TODO ADD ALTERNATIVE FOR doWithWebDescriptor as this method is removed now
   /* def doWithWebDescriptor = { xml ->

        def conf = SpringSecurityUtils.securityConfig
        if (!conf || !conf.cas.active) {
            return
        }

        // add the filter right after the last context-param
        def contextParam = xml.'context-param'
        contextParam[contextParam.size() - 1] + {
            'filter' {
                'filter-name'('CAS Validation Filter')
                'filter-class'(BannerSaml11ValidationFilter.name)
                'init-param' {
                    'param-name'('casServerUrlPrefix')
                    'param-value'(conf.cas.serverUrlPrefix)
                }
                'init-param' {
                    'param-name'('serverName')
                    'param-value'(conf.cas.serverName)
                }
                'init-param' {
                    'param-name'('redirectAfterValidation')
                    'param-value'('true')
                }
                'init-param' {
                    'param-name'('artifactParameterName')
                    'param-value'(conf.cas.artifactParameter)
                }
                'init-param' {
                    'param-name'('tolerance')
                    'param-value'(conf.cas.tolerance)
                }
            }
            'filter' {
                'filter-name'('CAS HttpServletRequest Wrapper Filter')
                'filter-class'(HttpServletRequestWrapperFilter.name)
            }
        }

        // add the filter-mapping right after the last filter
        def mappingLocation = xml.'filter'
        mappingLocation[mappingLocation.size() - 1] + {
            'filter-mapping' {
                'filter-name'('CAS Validation Filter')
                'url-pattern'('/*')
            }
            'filter-mapping' {
                'filter-name'('CAS HttpServletRequest Wrapper Filter')
                'url-pattern'('/*')
            }
        }

        def filterMapping = xml.'filter-mapping'
        filterMapping[filterMapping.size() - 1] + {
            'listener' {
                'listener-class'(SingleSignOutHttpSessionListener.name)
            }
        }
    } */


    Closure doWithSpring() { {->
        println "I am in Banner CAS"
            // TODO Implement runtime spring config (optional)
            def conf = SpringSecurityUtils.securityConfig
        println "**********************************In banner cas conf.saml ********************************************"
        println conf.cas
        println "*****************************************  **********************************************************"
            if (!conf || !conf.cas.active) {
                return
            }
            println '\nConfiguring Banner Spring Security CAS ...'

            casBannerAuthenticationProvider(CasAuthenticationProvider) {
                dataSource = ref(dataSource)
            }

            bannerCasAuthenticationFailureHandler(BannerCasAuthenticationFailureHandler){
                defaultFailureUrl = conf.failureHandler.defaultFailureUrl
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

            println '... finished configuring Banner Spring Security CAS\n'
        }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    void doWithApplicationContext() {
        // TODO Implement post initialization spring config (optional)
        // build providers list here to give dependent plugins a chance to register some
        def conf = SpringSecurityUtils.securityConfig
        if (!conf || !conf.cas.active) {
            return
        }
        def providerNames = []


        if (conf.providerNames) {
            providerNames.addAll conf.providerNames
        } else {
            //TODO After adding Banner_Core Dependency uncomment below lines.
            if(ControllerUtils.isGuestAuthenticationEnabled()){
                providerNames = ['casBannerAuthenticationProvider','selfServiceBannerAuthenticationProvider','bannerAuthenticationProvider']
            } else{
                providerNames = ['casBannerAuthenticationProvider']
            }
        }
        applicationContext.authenticationManager.providers = createBeanList(providerNames, applicationContext)

        // Define the spring security filters
        def authenticationProvider = Holders?.config?.banner.sso.authenticationProvider
        LinkedHashMap<String, String> filterChain = new LinkedHashMap();
        switch (authenticationProvider) {
            case 'cas':
                filterChain['/**/api/**'] = 'statelessSecurityContextPersistenceFilter,bannerMepCodeFilter,authenticationProcessingFilter,basicAuthenticationFilter,securityContextHolderAwareRequestFilter,anonymousProcessingFilter,basicExceptionTranslationFilter,filterInvocationInterceptor'
                filterChain['/**/qapi/**'] = 'statelessSecurityContextPersistenceFilter,bannerMepCodeFilter,authenticationProcessingFilter,basicAuthenticationFilter,securityContextHolderAwareRequestFilter,anonymousProcessingFilter,basicExceptionTranslationFilter,filterInvocationInterceptor'
                filterChain['/**'] = 'securityContextPersistenceFilter,logoutFilter,bannerMepCodeFilter,casAuthenticationFilter,authenticationProcessingFilter,securityContextHolderAwareRequestFilter,anonymousProcessingFilter,exceptionTranslationFilter,filterInvocationInterceptor'
                break
            default:
                break
        }

        List<SecurityFilterChain> chains = new ArrayList<SecurityFilterChain>()
        filterChain.each { key, value ->
            def filters = value.toString().split(',').collect {
                name -> applicationContext.getBean(name)
            }
            chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher(key), filters))
        }
        applicationContext.springSecurityFilterChain.filterChains = chains
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
