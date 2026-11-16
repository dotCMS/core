package com.liferay.portal.servlet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.security.ContentSecurityPolicyUtil;
import com.dotmarketing.util.Config;
import com.liferay.portal.ejb.CompanyLocalManagerUtil;
import com.liferay.portal.events.EventsProcessor;
import com.liferay.portal.model.Company;
import com.liferay.portal.struts.PortletRequestProcessor;
import com.liferay.portal.util.ContentUtil;
import com.liferay.portal.util.PortalInstances;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portal.util.ShutdownUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.Encryptor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.After;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Checks the portal servlet, which serves the back end under {@code /c}: it writes the back-end
 * {@code Content-Security-Policy} on its responses (the header filter does not match {@code /c},
 * because site pages under a root folder named {@code c} are served by CMSFilter on that path), and
 * it hands the request parameters to the Struts processor as the client sent them.
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
     * Method to test: {@link MainServlet#service(HttpServletRequest, HttpServletResponse)}
     * Given scenario: a running portal receives {@code /c/portal/layout?shuo=1&x=value-1}
     * Expected result: the request handed to the Struts processor still carries {@code x=value-1}
     */
    @Test
    public void testService_whenShuoParameterSent_shouldPassParametersAsSent() throws Exception {
        final Map<String, String[]> parameters = new LinkedHashMap<>();
        parameters.put("shuo", new String[] {"1"});
        parameters.put("x", new String[] {"value-1"});

        final HttpServletRequest processed = serveRunning(parameters);

        assertEquals("value-1", processed.getParameter("x"));
        assertArrayEquals(new String[] {"value-1"}, processed.getParameterValues("x"));
        assertArrayEquals(new String[] {"value-1"}, processed.getParameterMap().get("x"));
    }

    /**
     * Sends a {@code /c/portal/layout} request with the given parameters through a running portal
     * and returns the request the servlet hands to the Struts processor. The company has a real key,
     * so any change the servlet makes to the parameter values shows up in the returned request.
     */
    private HttpServletRequest serveRunning(final Map<String, String[]> parameters) throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/c");
        when(request.getPathInfo()).thenReturn("/portal/layout");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/c/portal/layout"));
        when(request.getSession()).thenReturn(mock(HttpSession.class));
        when(request.getParameterMap()).thenReturn(parameters);
        when(request.getParameterNames()).thenAnswer(invocation -> Collections.enumeration(parameters.keySet()));
        when(request.getParameter(anyString())).thenAnswer(invocation -> {
            final String[] values = parameters.get(invocation.<String>getArgument(0));
            return values == null ? null : values[0];
        });
        when(request.getParameterValues(anyString()))
                .thenAnswer(invocation -> parameters.get(invocation.<String>getArgument(0)));

        final ServletContext context = mock(ServletContext.class);
        when(context.getAttribute(WebKeys.PORTLET_STRUTS_PROCESSOR))
                .thenReturn(mock(PortletRequestProcessor.class));

        final Company company = mock(Company.class);
        when(company.getKeyObj()).thenReturn(Encryptor.generateKey());

        final MainServlet servlet = spy(new MainServlet());
        doReturn(context).when(servlet).getServletContext();
        final AtomicReference<HttpServletRequest> processed = new AtomicReference<>();
        doAnswer(invocation -> {
            processed.set(invocation.getArgument(0));
            return null;
        }).when(servlet).callParentService(any(HttpServletRequest.class), any(HttpServletResponse.class));

        try (MockedStatic<PortalInstances> portal = mockStatic(PortalInstances.class);
                MockedStatic<ShutdownUtil> shutdown = mockStatic(ShutdownUtil.class);
                MockedStatic<PropsUtil> props = mockStatic(PropsUtil.class);
                MockedStatic<CompanyLocalManagerUtil> companies = mockStatic(CompanyLocalManagerUtil.class);
                MockedStatic<PortalUtil> portalUtil = mockStatic(PortalUtil.class);
                MockedStatic<EventsProcessor> events = mockStatic(EventsProcessor.class)) {
            portal.when(PortalInstances::matches).thenReturn(true);
            shutdown.when(ShutdownUtil::isShutdown).thenReturn(false);
            props.when(() -> PropsUtil.getArray(anyString())).thenReturn(new String[0]);
            companies.when(() -> CompanyLocalManagerUtil.getCompany(any())).thenReturn(company);
            portalUtil.when(() -> PortalUtil.getUserId(any(HttpServletRequest.class))).thenReturn("user-1");
            servlet.service(request, mock(HttpServletResponse.class));
        }
        return processed.get();
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
