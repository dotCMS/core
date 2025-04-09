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
     * Method to test: {@link SpeedyAssetServlet#serve(HttpServletRequest, HttpServletResponse)}
     * Given Scenario: A request to the servlet with an authenticated user
     * Expected Result: The request should be forwarded for an authenticated user,
     * For a non-authenticated user, the status code should be 401
     */
    @Test
    public void speedyAssetWithAuthenticatedUser() throws Exception {

        final HttpServlet servlet = new SpeedyAssetServlet();
        ServletTestUtils.testServletWithAuthenticatedUser(servlet,
                assetId -> "/dotAsset/" + assetId);

    }

}
