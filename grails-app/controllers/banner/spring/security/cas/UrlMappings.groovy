/*******************************************************************************
 Copyright 2009-2018 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package banner.spring.security.cas
/**
 * Specifies all of the URL mappings supported by the application.
 */
class UrlMappings {

    static mappings = {

        "/ssb/uiCatalog/logout" (redirect : '/logout')

        "/ssb/menu" {
            controller = "selfServiceMenu"
            action = [GET: "data", POST: "create"]
        }

        "/ssb/i18n/$name*.properties"(controller: "i18n", action: "index" )


        "/ssb/resource/$controller" {
            action = [ GET: "list", POST: "create" ]
        }

        "/ssb/resource/$controller/batch" {
            action = [ POST: "processBatch" ]
        }


        "/ssb/resource/$controller/$id?" {
            action = [ GET: "show", PUT: "update", DELETE: "destroy" ]
            constraints {
                id(matches:/[0-9]+/)
            }
        }

        "/ssb/resource/$controller/sendToServer" {
            action = "sendToServer"
        }

        "/ssb/resource/$controller/$type" {
            action = "list"
            constraints {
                type(matches:/[^0-9]+/)
            }
        }

        "/ssb/resource/$controller/$type/batch" {
            action = [ POST: "processBatch" ]
            constraints {
                type(matches:/[^0-9]+/)
            }
        }

        "/ssb/$controller/$action?/$id?"{
            constraints {
                // apply constraints here
            }
        }

        "/login/auth" {
            controller = "login"
            action = "auth"
        }

        "/login/denied" {
            controller = "login"
            action = "denied"
        }

        "/login/authAjax" {
            controller = "login"
            action = "authAjax"
        }

        "/login/authfail" {
            controller = "login"
            action = "authfail"
        }

        "/logout" {
            controller = "logout"
            action = "index"
        }

        "/logout/timeout" {
            controller = "logout"
            action = "timeout"
        }


        "/logout/customLogout" {
            controller = "logout"
            action = "customLogout"
        }

        "/login/error" {
            controller = "login"
            action = "error"
        }

        "/"(view:"/index") {
            println "this is the map worked..."
        }

        "/index.gsp" (view:"/index")

        "500"(controller: "error", action: "internalServerError")
        "403"(controller: "error", action: "accessForbidden")
    }
}
