/* *****************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 ****************************************************************************** */

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.util.Holders
import net.hedtech.banner.security.CasAuthenticationProvider
import net.hedtech.jasig.cas.client.BannerSaml11ValidationFilter
import org.jasig.cas.client.session.SingleSignOutHttpSessionListener
import org.jasig.cas.client.util.HttpServletRequestWrapperFilter

class BannerSpringSecurityCasGrailsPlugin {

  // Note: the groupId 'should' be used when deploying this plugin via the 'grails maven-deploy --repository=snapshots' command,
    // however it is not being picked up.  Consequently, a pom.xml file is added to the root directory with the correct groupId
    // and will be removed when the maven-publisher plugin correctly sets the groupId based on the following field.
    String groupId = "net.hedtech"

    String version = '2.11.0'
    String grailsVersion = '2.5.0 > *'

    def dependsOn = [
            bannerCore: '1.0.17 => *'
    ]

    def doWithWebDescriptor = { xml ->

        def conf = SpringSecurityUtils.securityConfig
        if (!conf || !conf.cas.active) {
            return
        }

        println 'Configuring Spring Security CAS ...'

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
        casBannerAuthenticationProvider(CasAuthenticationProvider) {
            dataSource = ref(dataSource)
        }
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
                providerNames = ['casBannerAuthenticationProvider']
        }
        applicationContext.authenticationManager.providers = createBeanList(providerNames, applicationContext)
    }

    private def isSsbEnabled() {
        Holders.config.ssbEnabled instanceof Boolean ? Holders.config.ssbEnabled : false
    }

    private createBeanList(names, ctx) { names.collect { name -> ctx.getBean(name) } }

}