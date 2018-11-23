/*******************************************************************************
 Copyright 2009-2018 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

//TODO Commenting the imports and corresponding changes as imports in not supported here
//import net.hedtech.banner.configuration.ApplicationConfigurationUtils as ConfigFinder
//import grails.plugin.springsecurity.SecurityConfigType

// ******************************************************************************
//
//                       +++ EXTERNALIZED CONFIGURATION +++
//
// ******************************************************************************

grails.config.locations = [] // leave this initialized to an empty list, and add your locations in the map below.

//TODO Commenting the imports and corresponding changes as imports in not supported here
/*def locationAdder = ConfigFinder.&addLocation.curry(grails.config.locations)
println "App Name ${appName}"
[ BANNER_APP_CONFIG:        "banner_configuration.groovy",
//  BANNER_CORE_TESTAPP_CONFIG: "banner_core_testapp_configuration.groovy",
].each { envName, defaultFileName -> locationAdder( envName, defaultFileName ) }*/

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

cache.use_second_level_cache = true
cache.use_query_cache = false
cache.region.factory_class = 'org.hibernate.cache.SingletonEhCacheRegionFactory' // Hibernate 3
//    cache.region.factory_class = 'org.hibernate.cache.ehcache.SingletonEhCacheRegionFactory' // Hibernate 4
singleSession = true // configure OSIV singleSession mode
flush.mode = 'manual' // OSIV session flush mode outside of transactional context
