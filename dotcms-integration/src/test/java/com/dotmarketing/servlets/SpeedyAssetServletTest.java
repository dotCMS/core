package com.dotmarketing.servlets;

import com.dotcms.JUnit4WeldRunner;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotRuntimeException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServlet;

/**
 * Test for {@link SpeedyAssetServlet}
 */
@ApplicationScoped
@RunWith(JUnit4WeldRunner.class)
public class SpeedyAssetServletTest {

    /**
     * Prepare the testing environment
     */
    @BeforeClass
    public static void prepare()  {
        try {
            IntegrationTestInitService.getInstance().init();
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Method to test: {@link SpeedyAssetServlet#service(HttpServletRequest, HttpServletResponse)}
     * Given Scenario: A request to the servlet carrying an invalid and then a valid dotCMS credential
     * Expected Result: Neither is rejected with a 401 at the servlet; both are forwarded, with
     * permission enforcement delegated downstream (see dotCMS/core#35536).
     */
    @Test
    public void speedyAssetWithAuthenticatedUser() throws Exception {

        final HttpServlet servlet = new SpeedyAssetServlet();
        ServletTestUtils.testServletWithAuthenticatedUser(servlet,
                assetId -> "/dotAsset/" + assetId);

    }

    /**
     * Method to test: {@link SpeedyAssetServlet#service(HttpServletRequest, HttpServletResponse)}
     * Given Scenario: A request for a publicly-readable (anonymous READ) asset carries a foreign
     * BASIC Authorization header whose credentials are not a valid dotCMS user (as a browser would
     * replay from an upstream Basic-Auth gating layer per RFC 7617).
     * Expected Result: The servlet falls through to anonymous and serves (forwards) the asset
     * instead of returning a 401. Regression test for dotCMS/core#35536.
     */
    @Test
    public void speedyAssetPublicServedWithForeignBasicAuth() throws Exception {

        final HttpServlet servlet = new SpeedyAssetServlet();
        ServletTestUtils.testPublicAssetServedWithForeignBasicAuth(servlet,
                assetId -> "/dotAsset/" + assetId);

    }

}
