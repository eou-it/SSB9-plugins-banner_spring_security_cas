/*******************************************************************************
 Copyright 2009-2012 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

import net.hedtech.banner.configuration.ApplicationConfigurationUtils as ConfigFinder
import grails.plugin.springsecurity.SecurityConfigType

// ******************************************************************************
//
//                       +++ EXTERNALIZED CONFIGURATION +++
//
// ******************************************************************************

grails.config.locations = [] // leave this initialized to an empty list, and add your locations in the map below.
def locationAdder = ConfigFinder.&addLocation.curry(grails.config.locations)
println "App Name ${appName}"
[ BANNER_APP_CONFIG:        "banner_configuration.groovy",
//  BANNER_CORE_TESTAPP_CONFIG: "banner_core_testapp_configuration.groovy",
].each { envName, defaultFileName -> locationAdder( envName, defaultFileName ) }
grails.config.locations.each {
    println "configuration: " + it
}

// ******************************************************************************
// ****** SSB specific db related flags
ssbEnabled = true
ssbOracleUsersProxied = false
// ******************************************************************************

// ******************************************************************************
// Miscellaneous
grails.project.groupId = "net.hedtech" // used when deploying to a maven repo

grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [
        html: ['text/html', 'application/xhtml+xml'],
        xml: ['text/xml', 'application/xml', 'application/vnd.sungardhe.student.v0.01+xml'],
        text: 'text/plain',
        js: 'text/javascript',
        rss: 'application/rss+xml',
        atom: 'application/atom+xml',
        css: 'text/css',
        csv: 'text/csv',
        all: '*/*',
        json: ['application/json', 'text/json'],
        form: 'application/x-www-form-urlencoded',
        multipartForm: 'multipart/form-data',
        jpg: 'image/jpeg',
        png: 'image/png',
        gif: 'image/gif',
        bmp: 'image/bmp',
        svg:'image/svg+xml',
        svgz:'image/svg+xml'
]

// The default codec used to encode data with ${}
grails.views.default.codec = "html" // none, html, base64  **** note: Setting this to html will ensure html is escaped, to prevent XSS attack ****
grails.views.gsp.encoding = "UTF-8"
grails.converters.encoding = "UTF-8"

grails.converters.domain.include.version = true
//grails.converters.json.date = "default"

grails.converters.json.pretty.print = true
grails.converters.json.default.deep = true

// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = false

// enable GSP preprocessing: replace head -> g:captureHead, title -> g:captureTitle, meta -> g:captureMeta, body -> g:captureBody
grails.views.gsp.sitemesh.preprocess = true

grails.resources.mappers.yuicssminify.includes = ['**/*.css']
grails.resources.mappers.yuijsminify.includes  = ['**/*.js']
grails.resources.mappers.yuicssminify.excludes = ['**/*.min.css']
grails.resources.mappers.yuijsminify.excludes  = ['**/*.min.js']

environments {
    development {
        grails.resources.debug = true
    }
}


// ******************************************************************************
//
//                       +++ DATA ORIGIN CONFIGURATION +++
//
// ******************************************************************************
// This field is a Banner standard, along with 'lastModifiedBy' and lastModified.
// These properties are populated automatically before an entity is inserted or updated
// within the database. The lastModifiedBy uses the username of the logged in user,
// the lastModified uses the current timestamp, and the dataOrigin uses the value
// specified here:
dataOrigin = "Banner"

// ******************************************************************************
//
//                       +++ FORM-CONTROLLER MAP +++
//
// ******************************************************************************
// This map relates controllers to the Banner forms that it replaces.  This map
// supports 1:1 and 1:M (where a controller supports the functionality of more than
// one Banner form.  This map is critical, as it is used by the security framework to
// set appropriate Banner security role(s) on a database connection. For example, if a
// logged in user navigates to the 'medicalInformation' controller, when a database
// connection is attained and the user has the necessary role, the role is enabled
// for that user and Banner object.
formControllerMap = [
        '/':['GUAGMNU','SELFSERVICE']
]

banner {
    sso {
        authenticationProvider = 'cas'
        authenticationAssertionAttribute = 'UDC_IDENTIFIER'
    }
}

grails {
    plugin {
        springsecurity {
            cas {
                active = true
                serverUrlPrefix = 'https://e004060.ellucian.com:8943/cas'
                serviceUrl = 'http://localhost:8090/banner-third-party-authenticator/j_spring_cas_security_check'
                serverName = 'http://localhost:8090'
                proxyCallbackUrl = 'http://localhost:8090/banner-third-party-authenticator/secure/receptor'
                loginUri = '/login'
                sendRenew = false
                proxyReceptorUrl = '/secure/receptor'
                useSingleSignout = true
                key = 'grails-spring-security-cas'
                artifactParameter = 'ticket'
                serviceParameter = 'service'
                filterProcessesUrl = '/j_spring_cas_security_check'
                serverUrlEncoding = 'UTF-8'
            }
            logout {
                afterLogoutUrl = "/"
                mepErrorLogoutUrl='/logout/logoutPage'
            }
            useRequestMapDomainClass = false
            securityConfigType = SecurityConfigType.InterceptUrlMap
            interceptUrlMap = [
                    '/': ['ROLE_SELFSERVICE-ALLROLES_BAN_DEFAULT_M', 'ROLE_SELFSERVICE-STUDENT_BAN_DEFAULT_M'],
                    '/login/**': ['IS_AUTHENTICATED_ANONYMOUSLY'],
                    '/logout/**': ['IS_AUTHENTICATED_ANONYMOUSLY'],
                    '/index': ['IS_AUTHENTICATED_ANONYMOUSLY'],
                    '/**': ['IS_AUTHENTICATED_ANONYMOUSLY']
            ]
        }
    }
}

// CodeNarc rulesets
codenarc.ruleSetFiles="rulesets/banner.groovy"
codenarc.reportName="target/CodeNarcReport.html"
codenarc.propertiesFile="grails-app/conf/codenarc.properties"
codenarc.extraIncludeDirs=["grails-app/composers"]

grails.validateable.packages=['net.hedtech.banner.student.registration']

// local seeddata files
seedDataTarget = [ ]

markdown = [
        removeHtml: true
]

String loggingFileDir =  "target/logs"
String logAppName = "banner-third-party-authenticator"
String loggingFileName = "${loggingFileDir}/${logAppName}.log".toString()
def environment = grails.util.Environment.current

log4j = {
    appenders {
        rollingFile name: 'appLog', file: loggingFileName, maxFileSize: "${10 * 1024 * 1024}", maxBackupIndex: 10, layout: pattern(conversionPattern: '%d{[EEE, dd-MMM-yyyy @ HH:mm:ss.SSS]} [%t] %-5p %c %x - %m%n')
    }

    switch (environment) {
        case grails.util.Environment.DEVELOPMENT:
            root {
                error 'stdout', 'appLog'
                additivity = true
            }
            error 'com.sungardhe.banner.representations'
            error 'com.sungardhe.banner.supplemental.SupplementalDataService'
            break
        case grails.util.Environment.TEST:
            root {
                error 'stdout', 'appLog'
                additivity = true
            }
            break
        case grails.util.Environment.PRODUCTION:
            all {
                debug 'stdout', 'appLog'
                additivity = true
            }
            error 'grails.app.service'
            error 'grails.app.controller'
            info 'com.sungardhe.banner.representations'
            info 'com.sungardhe.banner.supplemental.SupplementalDataService'
            break
    }
    trace 'org.springframework.security.cas'
    trace 'org.jasig.cas.client'
    debug 'net.hedtech.banner.security.CasAuthenticationProvider'
    trace 'net.hedtech.restfulapi.RestfulApiController'
}