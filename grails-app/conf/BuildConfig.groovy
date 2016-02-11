grails.servlet.version = "3.0" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
grails.project.source.level = 1.6
//grails.project.war.file = "target/${appName}-${appVersion}.war"

grails.plugin.location.'banner-core' = "../banner_core.git"


grails.project.dependency.resolver = "maven" // or ivy
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // specify dependency exclusions here; for example, uncomment this to disable ehcache:
        // excludes 'ehcache'
    }
    log "error" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve
    legacyResolve false // whether to do a secondary resolve on plugin installation, not advised and here for backwards compatibility

    repositories {
        inherits true // Whether to inherit repository definitions from plugins

        grailsPlugins()
        grailsHome()
        mavenLocal()
        grailsCentral()
        mavenCentral()
        mavenRepo "http://repo.spring.io/milestone/"

        // uncomment these (or add new ones) to enable remote dependency resolution from public Maven repositories
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
    }

    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes e.g.
        // runtime 'mysql:mysql-connector-java:5.1.29'
        // runtime 'org.postgresql:postgresql:9.3-1101-jdbc41'
        compile 'joda-time:joda-time:2.2'
        compile 'org.opensaml:xmltooling:1.4.4'
        compile 'org.opensaml:opensaml:2.6.4'

        compile ('org.jasig.cas.client:cas-client-core:3.3.3') {
            excludes 'commons-codec', 'commons-logging', 'junit', 'log4j', 'servlet-api',
                    'spring-beans', 'spring-context', 'spring-core', 'spring-test', 'xmlsec'
        }
        test "org.grails:grails-datastore-test-support:1.0.2-grails-2.4"

        runtime "org.apache.santuario:xmlsec:1.4.3"
    }

    plugins {
        compile ':resources:1.2.8'
        compile ":spring-security-cas:2.0-RC1"
    }
}
