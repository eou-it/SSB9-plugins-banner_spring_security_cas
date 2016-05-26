/*******************************************************************************
 Copyright 2009-2016 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.security

import grails.util.Holders
import net.hedtech.banner.testing.BaseIntegrationTestCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes


/**
 * Tests that the ValidPropertyConstraint is working as expected.
 */
class CasAuthenticationProviderIntegrationTests extends BaseIntegrationTestCase {

    def casAuthenticationProvider

    @Before
    public void setUp() {
        Holders?.config.banner.sso.authenticationAssertionAttribute = "UDC_IDENTIFIER"

        casAuthenticationProvider = new CasAuthenticationProvider()
        formContext = ['GUAGMNU']
        super.setUp()
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

}