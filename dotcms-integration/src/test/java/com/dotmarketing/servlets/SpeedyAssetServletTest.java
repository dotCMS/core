package com.dotmarketing.servlets;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotRuntimeException;
import org.junit.BeforeClass;
import org.junit.Test;
import javax.servlet.http.HttpServlet;

/**
 * Test for {@link SpeedyAssetServlet}
 */

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
