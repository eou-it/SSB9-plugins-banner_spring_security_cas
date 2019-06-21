/*******************************************************************************
 Copyright 2009-2019 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.security

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.util.Holders
import net.hedtech.banner.testing.BaseIntegrationTestCase
import org.jasig.cas.client.authentication.AttributePrincipal
import org.jasig.cas.client.authentication.AttributePrincipalImpl
import org.jasig.cas.client.util.AbstractCasFilter
import org.jasig.cas.client.validation.AssertionImpl
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import static groovy.test.GroovyAssert.shouldFail

import javax.servlet.http.HttpSession

/**
 * Tests that the ValidPropertyConstraint is working as expected.
 */
@Integration
@Rollback
class CasAuthenticationProviderIntegrationTests extends BaseIntegrationTestCase {

    def casAuthenticationProvider

    def dataSource

    def sql

    def conn
    @Before
    public void setUp() {
        formContext = ['GUAGMNU']
        super.setUp()
        Holders?.config.banner.sso.authenticationAssertionAttribute = "UDC_IDENTIFIER"
        casAuthenticationProvider = new CasAuthenticationProvider()
        casAuthenticationProvider.dataSource = this.dataSource
       /* conn = casAuthenticationProvider.dataSource.getSsbConnection()
        sql = new Sql(conn)*/
    }

    @After
    public void tearDown() {
        super.tearDown()
    }

    @Test
    void testSupportsTrue() {
        Holders?.config.banner.sso.authenticationProvider = "cas"

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/ssb/home");

        request.addHeader("UDC_IDENTIFIER", "999XE999")

        Holders?.config.banner.sso.excludedUrlPattern = "/guest"
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request))

        assertTrue casAuthenticationProvider.supports(this.class)
    }

    @Test
    void testSupportsFalse() {
        Holders?.config.banner.sso.authenticationProvider = "test"

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/ssb/home");

        request.addHeader("UDC_IDENTIFIER", "999XE999")

        Holders?.config.banner.sso.excludedUrlPattern = "/guest"
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request))

        assertFalse casAuthenticationProvider.supports(this.class)
    }

    @Test
    void testIsNotExcludedFromSSOTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/ssb/home");

        request.addHeader("UDC_IDENTIFIER", "999XE999")

        Holders?.config.banner.sso.excludedUrlPattern = "/guest"
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request))

        assertTrue casAuthenticationProvider.isNotExcludedFromSSO()
    }

    @Test
    void testIsNotExcludedFromSSOFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/ssb/guest");

        request.addHeader("UDC_IDENTIFIER", "999XE999")

        Holders?.config.banner.sso.excludedUrlPattern = "/guest"

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request))

        assertFalse casAuthenticationProvider.isNotExcludedFromSSO()
    }

    @Test
    void testIsCasEnabledTrue() {
        Holders?.config.banner.sso.authenticationProvider = "cas"
        assertTrue casAuthenticationProvider.isCasEnabled()
    }

    @Test
    void testIsCasEnabledFalse() {
        Holders?.config.banner.sso.authenticationProvider = "test"
        assertFalse casAuthenticationProvider.isCasEnabled()
    }

    @Test
    void testCasAuthentificationSuccess(){
        Holders.config.defaultWebSessionTimeout = 3000
        Holders?.config.banner.sso.authenticationProvider = "cas"
        Map<String, Object> map = new HashMap<String, Object>(1);
        map.put("UDC_IDENTIFIER", "INTEGRATION_TEST_SAML_DISABLED");
        AttributePrincipal principal = new AttributePrincipalImpl("GRAILS_USER", map);
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpSession session = request.getSession()
        session.setAttribute(AbstractCasFilter.CONST_CAS_ASSERTION, new AssertionImpl(principal));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request))
        Authentication auth = new UsernamePasswordAuthenticationToken("GRAILS_USER", "u_pick_it")
        casAuthenticationProvider.authenticate(auth)
    }

    @Test
    void testCasBadCredentialExpection(){
        Holders.config.defaultWebSessionTimeout = 3000
        Holders?.config.banner.sso.authenticationProvider = "cas"
        Map<String, Object> map = new HashMap<String, Object>(1);
        map.put("UDC_IDENTIFIER", "999XE999");
        AttributePrincipal principal = new AttributePrincipalImpl("GRAILS_USER", map);
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpSession session = request.getSession()
        session.setAttribute(AbstractCasFilter.CONST_CAS_ASSERTION, new AssertionImpl(principal));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request))
        Authentication auth = new UsernamePasswordAuthenticationToken("GRAILS_USER", "u_pick_it")
        shouldFail(BadCredentialsException) {
            casAuthenticationProvider.authenticate(auth)
        }
    }

    @Test
    void testCasAuthentificationFailure(){
        Holders?.config.banner.sso.authenticationProvider = "cas"
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpSession session = request.getSession()
        session.setAttribute(AbstractCasFilter.CONST_CAS_ASSERTION, new AssertionImpl("999XE999"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request))
        Authentication auth = new UsernamePasswordAuthenticationToken("GRAILS_USER", "u_pick_it")
        shouldFail(UsernameNotFoundException) {
            casAuthenticationProvider.authenticate(auth)
        }
    }

}