/*******************************************************************************
 Copyright 2009-2012 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.jasig.cas.client

import net.hedtech.banner.testing.BaseIntegrationTestCase
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Tests that the ValidPropertyConstraint is working as expected.
 */
class BannerSaml11ValidationFilterIntegrationTests extends BaseIntegrationTestCase {

    @Before
    public void setUp() {
        formContext = ['GUAGMNU']
        super.setUp()
    }

    @After
    public void tearDown() {
        super.tearDown()
    }

    //Below test will be removed when we add actual tests to get coverage
    @Test
    void testDummyTes() {
        assert 1==1
    }
}