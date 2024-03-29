/*******************************************************************************
 Copyright 2017-2019 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.security

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.util.Holders
import groovy.sql.Sql
import net.hedtech.banner.testing.BaseIntegrationTestCase
import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.plugins.testing.GrailsMockHttpServletResponse
import org.easymock.EasyMock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.RedirectStrategy
import org.springframework.security.web.WebAttributes

/**
 * Integration test cases for BannerCasAuthenticationFailureHandler.
 */
@Integration
@Rollback
class BannerCasAuthenticationFailureHandlerIntegrationTests extends BaseIntegrationTestCase {

    BannerCasAuthenticationFailureHandler bannerCasAuthenticationFailureHandler
    GrailsMockHttpServletRequest request
    GrailsMockHttpServletResponse response
    AuthenticationException e
    def authenticationDataSource

    private static final String MSG = 'TEST_FAILURE_MESSAGE'
    private static final String MODULE = 'TEST_FALURE_MODULE'
    private static final String AUTH_NAME = 'TEST_FAILURE_AUTH_NAME'

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
        deleteAllFailureHandlerDataFromDB()
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

    @Test
    public void testVerifyTheDataInDB() throws Exception {
        bannerCasAuthenticationFailureHandler.setDefaultFailureUrl("/target")

        request.session.setAttribute('msg', MSG)
        request.session.setAttribute('module', MODULE)
        request.session.setAttribute('auth_name', AUTH_NAME)

        bannerCasAuthenticationFailureHandler.onAuthenticationFailure(request, response, e)
        assert (request.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION) == e)
        assert (response.getRedirectedUrl() == "/target")

        final String SELECT_QUERY = 'SELECT * FROM GURALOG ' +
                'WHERE GURALOG_OBJECT = ? ' +
                'AND GURALOG_USERID = ? ' +
                'AND GURALOG_REASON = ? '

        def sql
        try {
            def conn = authenticationDataSource.getConnection('BANSECR', 'u_pick_it')
            sql = new Sql(conn)
            sql.eachRow(SELECT_QUERY, [MODULE, AUTH_NAME, MSG], {
                assertEquals(it.GURALOG_OBJECT, MODULE)
                assertEquals(it.GURALOG_USERID, AUTH_NAME)
                assertEquals(it.GURALOG_REASON, MSG)
            })
        } catch (e) {
        } finally {
            sql?.close()
        }
    }

    private void deleteAllFailureHandlerDataFromDB() {
        final String updateStatement = 'DELETE FROM GURALOG ' +
                'WHERE GURALOG_OBJECT = ? ' +
                'AND GURALOG_USERID = ? ' +
                'AND GURALOG_REASON = ?'
        def sql
        try {
            def conn = authenticationDataSource.getConnection('BANSECR', 'u_pick_it')
            sql = new Sql(conn)
            sql.executeUpdate(updateStatement, [MODULE, AUTH_NAME, MSG])
            sql.commit()
        } catch (e) {
        } finally {
            sql?.close()
        }
    }
}
