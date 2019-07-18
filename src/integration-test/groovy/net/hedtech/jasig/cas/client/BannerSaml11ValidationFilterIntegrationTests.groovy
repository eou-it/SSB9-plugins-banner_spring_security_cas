/*******************************************************************************
 Copyright 2009-2019 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.jasig.cas.client

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import net.hedtech.banner.testing.BaseIntegrationTestCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.mock.web.MockFilterConfig
import org.springframework.mock.web.MockServletContext

import javax.servlet.FilterConfig
import javax.servlet.ServletContext

/**
 * Tests that the BannerSaml11ValidationFilterIntegrationTests is working as expected.
 */
@Integration
@Rollback
class BannerSaml11ValidationFilterIntegrationTests extends BaseIntegrationTestCase {

    def bannerSaml11ValidationFilter

    @Before
    public void setUp() {
        formContext = ['GUAGMNU']
        super.setUp()

        bannerSaml11ValidationFilter =  new BannerSaml11ValidationFilter()
    }

    @After
    public void tearDown() {
        super.tearDown()
    }

    @Test
    void testGetTicketValidator() {
        ServletContext servletContext = new MockServletContext();
        FilterConfig filterConfig = new MockFilterConfig(servletContext);
        filterConfig.addInitParameter("casServerUrlPrefix", "https://localhost:8443/cas")
        assertNotNull bannerSaml11ValidationFilter.getTicketValidator(filterConfig)
    }
}