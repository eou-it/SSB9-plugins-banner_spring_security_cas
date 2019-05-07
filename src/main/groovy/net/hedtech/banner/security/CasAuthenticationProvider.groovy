/*******************************************************************************
 Copyright 2009-2018 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.security

import grails.util.Holders
import grails.util.Holders  as CH
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import net.hedtech.banner.exceptions.AuthorizationException
import net.hedtech.banner.general.audit.LoginAuditService
import org.jasig.cas.client.util.AbstractCasFilter
import org.springframework.security.authentication.*
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.context.request.RequestContextHolder as RCH

/**
 * An authentication provider for Banner that authenticates a user using CAS.
 */
@Slf4j
public class CasAuthenticationProvider implements AuthenticationProvider {

    def dataSource  // injected by Spring

    public boolean supports( Class clazz ) {
        log.trace "CasBannerAuthenticationProvider.supports( $clazz ) will return ${isCasEnabled()}"
        isCasEnabled() && isNotExcludedFromSSO()
    }


    public boolean isNotExcludedFromSSO() {
        def theUrl = RCH.currentRequestAttributes().request.forwardURI
        def excludedUrlPattern = ''
        excludedUrlPattern = CH.config.banner.sso.excludedUrlPattern?.toString() ?:'[:]' // e.g., 'guest'
        !("$theUrl"?.contains( excludedUrlPattern ))
    }


    public boolean isCasEnabled() {
        def casEnabled = CH?.config.banner.sso.authenticationProvider
        'cas'.equalsIgnoreCase( casEnabled )
    }

    public Authentication authenticate( Authentication authentication ) {

        def conn
        try {
            conn = dataSource.unproxiedConnection
            Sql db = new Sql( conn )

            log.trace "CasAuthenticationProvider.casAuthentication doing CAS authentication"
            def sessionObj = RCH.currentRequestAttributes().request.session
            sessionObj.setAttribute("auth_name", sessionObj.getAttribute( AbstractCasFilter.CONST_CAS_ASSERTION ).principal.name)
            def attributeMap = sessionObj.getAttribute( AbstractCasFilter.CONST_CAS_ASSERTION ).principal.attributes
            def assertAttributeValue = attributeMap[CH?.config?.banner.sso.authenticationAssertionAttribute]

            if(assertAttributeValue == null) {
                log.error("System is configured for CAS authentication and identity assertion is $assertAttributeValue")  // NULL
                throw new UsernameNotFoundException("System is configured for CAS authentication and identity assertion is $assertAttributeValue")
            }

            def dbUser = AuthenticationProviderUtility.getMappedUserForUdcId(assertAttributeValue, dataSource)
            log.debug "CasAuthenticationProvider.casAuthentication found Oracle database user $dbUser for assertAttributeValue"


            String loginAuditConfiguration = AuthenticationProviderUtility.getLoginAuditConfiguration()

            if(dbUser!= null && loginAuditConfiguration?.equalsIgnoreCase('Y')){
                String loginComment = "Login successful"
                LoginAuditService loginAuditService = null
                if (!loginAuditService) {
                    loginAuditService = Holders.grailsApplication.mainContext.getBean("loginAuditService")
                }
                loginAuditService.createLoginLogoutAudit(dbUser,loginComment)
            }

            // Next, we'll verify the authenticationResults (and throw appropriate exceptions for expired pin, disabled account, etc.)
            AuthenticationProviderUtility.verifyAuthenticationResults this, authentication, dbUser
            log.debug "CasAuthenticationProvider.authenticate verify authentication results"

            BannerAuthenticationToken bannerAuthenticationToken = AuthenticationProviderUtility.createAuthenticationToken(dbUser,dataSource, this)
            log.debug "CasAuthenticationProvider.casAuthentication BannerAuthenticationToken updated with claims $bannerAuthenticationToken"

            def applicationContext = CH?.applicationContext
            applicationContext.publishEvent( new BannerAuthenticationEvent( dbUser.name, true, '', '', new Date(), '' ) )

            bannerAuthenticationToken
        }
        catch (DisabledException de)           {
            log.error "CasAuthenticationProvider was not able to authenticate user $authentication.name, due to DisabledException: ${de.message}"
            throw de
        } catch (CredentialsExpiredException ce) {
            log.error "CasAuthenticationProvider was not able to authenticate user $authentication.name, due to CredentialsExpiredException: ${ce.message}"
            throw ce
        } catch (LockedException le)             {
            log.error "CasAuthenticationProvider was not able to authenticate user $authentication.name, due to LockedException: ${le.message}"
            throw le
        } catch(AuthorizationException ae) {
            log.error "CasAuthenticationProvider was not able to authenticate user $authentication.name, due to AuthorizationException: ${ae.message}"
            throw ae
        } catch (BadCredentialsException be)     {
            log.error "CasAuthenticationProvider was not able to authenticate user $authentication.name, due to BadCredentialsException: ${be.message}"
            throw be
        }catch (UsernameNotFoundException ue)     {
            log.error "CasAuthenticationProvider was not able to authenticate user $authentication.name, due to UsernameNotFoundException: ${ue.message}"
            throw ue
        }
        catch (e) {
            // We don't expect an exception here, as failed authentication should be reported via the above exceptions
            log.error "CasAuthenticationProvider was not able to authenticate user $authentication.name, due to exception: ${e.message}"
            return null // this is a rare situation where we want to bury the exception
        } finally {
            conn?.close()
        }
    }
}
