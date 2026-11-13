package com.liferay.portal.servlet;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.security.ContentSecurityPolicyUtil;
import com.dotmarketing.util.Config;
import com.liferay.portal.util.ContentUtil;
import com.liferay.portal.util.PortalInstances;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Checks that the portal servlet, which serves the back end under {@code /c}, writes the back-end
 * {@code Content-Security-Policy} on its responses. The header filter does not match {@code /c},
 * because site pages under a root folder named {@code c} are served by CMSFilter on that path.
 */
public class MainServletTest extends UnitTestBase {

    private static final String CSP_HEADER = "Content-Security-Policy";

    /** Clears the property so each test starts from the shipped configuration. */
    @After
    public void tearDown() {
        Config.setProperty(ContentSecurityPolicyUtil.BACKEND_ENABLED, null);
    }

    /**
     * Method to test: {@link MainServlet#service(HttpServletRequest, HttpServletResponse)}
     * Given scenario: nothing configured; a request for {@code /c/portal/layout} reaches the servlet
     * Expected result: the response carries the default back-end policy
     */
    @Test
    public void testService_whenPortalRequest_shouldSendBackendPolicy() throws Exception {
        verify(serve()).setHeader(CSP_HEADER, ContentSecurityPolicyUtil.DEFAULT_BACKEND_POLICY);
    }

    /**
     * Method to test: {@link MainServlet#service(HttpServletRequest, HttpServletResponse)}
     * Given scenario: the back-end policy is switched off
     * Expected result: the response carries no policy header
     */
    @Test
    public void testService_whenDisabled_shouldNotSendPolicy() throws Exception {
        Config.setProperty(ContentSecurityPolicyUtil.BACKEND_ENABLED, false);

        verify(serve(), never()).setHeader(eq(CSP_HEADER), anyString());
    }

    /**
     * Sends a {@code /c/portal/layout} request to the servlet while the portal reports itself as
     * still starting, so the servlet answers with its start-up page and touches nothing else.
     */
    private HttpServletResponse serve() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/c");
        when(request.getPathInfo()).thenReturn("/portal/layout");
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));

        try (MockedStatic<PortalInstances> portal = mockStatic(PortalInstances.class);
                MockedStatic<ContentUtil> content = mockStatic(ContentUtil.class)) {
            portal.when(PortalInstances::matches).thenReturn(false);
            content.when(() -> ContentUtil.get(anyString())).thenReturn("");
            new MainServlet().service(request, response);
        }
        return response;
    }

}
