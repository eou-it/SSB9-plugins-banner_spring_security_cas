/* *****************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 ****************************************************************************** */

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.util.Holders
import net.hedtech.banner.controllers.ControllerUtils
import net.hedtech.banner.security.CasAuthenticationFailureHandler
import net.hedtech.banner.security.CasAuthenticationProvider
import net.hedtech.jasig.cas.client.BannerSaml11ValidationFilter
import org.jasig.cas.client.session.SingleSignOutHttpSessionListener
import org.jasig.cas.client.util.HttpServletRequestWrapperFilter
import org.springframework.security.cas.web.CasAuthenticationFilter
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher

import javax.servlet.Filter

class BannerSpringSecurityCasGrailsPlugin {

  // Note: the groupId 'should' be used when deploying this plugin via the 'grails maven-deploy --repository=snapshots' command,
    // however it is not being picked up.  Consequently, a pom.xml file is added to the root directory with the correct groupId
    // and will be removed when the maven-publisher plugin correctly sets the groupId based on the following field.
    String groupId = "net.hedtech"

    String version = '9.17.2'
    String grailsVersion = '2.5.0 > *'

    def dependsOn = [
            bannerCore: '1.0.17 => *'
    ]

    def doWithWebDescriptor = { xml ->

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
    }

    def doWithSpring = {
        def conf = SpringSecurityUtils.securityConfig
        if (!conf || !conf.cas.active) {
            return
        }
        println '\nConfiguring Banner Spring Security CAS ...'

        casBannerAuthenticationProvider(CasAuthenticationProvider) {
            dataSource = ref(dataSource)
        }

        casAuthenticationFailureHandler(CasAuthenticationFailureHandler){
            defaultFailureUrl = SpringSecurityUtils.securityConfig.failureHandler.defaultFailureUrl
        }

        casAuthenticationFilter(CasAuthenticationFilter){
            authenticationManager = ref('authenticationManager')
            sessionAuthenticationStrategy = ref('sessionAuthenticationStrategy')
            authenticationSuccessHandler = ref('authenticationSuccessHandler')
            authenticationFailureHandler = ref('casAuthenticationFailureHandler')
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

    def doWithApplicationContext = { applicationContext ->
        // build providers list here to give dependent plugins a chance to register some
        def conf = SpringSecurityUtils.securityConfig
        if (!conf || !conf.cas.active) {
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

        LinkedHashMap<RequestMatcher, List<Filter>> filterChainMap = new LinkedHashMap()
        filterChain.each { key, value ->
            def filters = value.toString().split(',').collect {
                name -> applicationContext.getBean(name)
            }
            filterChainMap[new AntPathRequestMatcher(key)] = filters
        }
        applicationContext.springSecurityFilterChain.filterChainMap = filterChainMap
    }

    private def isSsbEnabled() {
        Holders.config.ssbEnabled instanceof Boolean ? Holders.config.ssbEnabled : false
    }

    private createBeanList(names, ctx) { names.collect { name -> ctx.getBean(name) } }

}
