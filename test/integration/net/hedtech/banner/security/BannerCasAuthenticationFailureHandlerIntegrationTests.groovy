/*******************************************************************************
 Copyright 2017 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.security

import grails.util.Holders
import net.hedtech.banner.testing.BaseIntegrationTestCase
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletResponse
import org.easymock.EasyMock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.RedirectStrategy
import org.springframework.security.web.WebAttributes

/**
 * BannerCasAuthenticationFailureHandlerIntegrationTests.
 *
 */
class BannerCasAuthenticationFailureHandlerIntegrationTests extends BaseIntegrationTestCase {

    BannerCasAuthenticationFailureHandler bannerCasAuthenticationFailureHandler
    GrailsMockHttpServletRequest request
    GrailsMockHttpServletResponse response
    AuthenticationException e

    @Before
    public void setUp() {
        Holders?.config.banner.sso.authenticationAssertionAttribute = "UDC_IDENTIFIER"
        formContext = ['GUAGMNU']
        bannerCasAuthenticationFailureHandler = new BannerCasAuthenticationFailureHandler()
        request = new GrailsMockHttpServletRequest()
        response = new GrailsMockHttpServletResponse()
        e = EasyMock.createMock(AuthenticationException.class)
        super.setUp()
    }

    @After
    public void tearDown() {
        super.tearDown()
    }

    @Test
    public void error401IsReturnedIfNoUrlIsSet() throws Exception {
        RedirectStrategy rs = EasyMock.createMock(RedirectStrategy.class)
        bannerCasAuthenticationFailureHandler.setRedirectStrategy(rs)
        assert (bannerCasAuthenticationFailureHandler.getRedirectStrategy() == rs)

        bannerCasAuthenticationFailureHandler.onAuthenticationFailure(request, response, e)
        assert (response.getStatus() == 401)
    }

    @Test
    public void exceptionIsSavedToSessionOnRedirect() throws Exception {
        bannerCasAuthenticationFailureHandler.setDefaultFailureUrl("/target")

        bannerCasAuthenticationFailureHandler.onAuthenticationFailure(request, response, e)
        assert (request.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION) == e)
        assert (response.getRedirectedUrl() == "/target")
    }

    @Test
    public void responseIsForwardedIfUseForwardIsTrue() throws Exception {
        BannerCasAuthenticationFailureHandler afh = new BannerCasAuthenticationFailureHandler()
        afh.setDefaultFailureUrl("/target")
        afh.setUseForward(true)
        assertTrue(afh.isUseForward())

        afh.onAuthenticationFailure(request, response, e)
        assertNull(response.getRedirectedUrl())
        assertEquals(response.getForwardedUrl(), "/target")

        // Request scope should be used for forward
        assertSame(request.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION), e)
    }
}
