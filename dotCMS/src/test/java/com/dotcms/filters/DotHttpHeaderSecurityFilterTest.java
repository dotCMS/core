package com.dotcms.filters;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.EnvironmentVariablesService;
import com.dotmarketing.util.Logger;
import java.util.Collections;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * Checks the {@code Strict-Transport-Security} header written by {@link DotHttpHeaderSecurityFilter}
 * for the default, a configured value, unusable values (set as a property or as the
 * {@code DOT_HSTS_MAX_AGE_SECONDS} environment variable) and an explicit {@code web.xml} init-param,
 * and the {@code Content-Security-Policy} header it writes on back-end paths.
 */
public class DotHttpHeaderSecurityFilterTest extends UnitTestBase {

    private static final String HSTS_HEADER = "Strict-Transport-Security";

    private static final String HSTS_ENV_VAR = "DOT_HSTS_MAX_AGE_SECONDS";

    private static final String CSP_HEADER = "Content-Security-Policy";

    private static final String CSP_REPORT_ONLY_HEADER = "Content-Security-Policy-Report-Only";

    private static final String CSP_ENABLED = "CONTENT_SECURITY_POLICY_BACKEND_ENABLED";

    private static final String CSP_POLICY = "CONTENT_SECURITY_POLICY_BACKEND_HEADER";

    private static final String CSP_REPORT_ONLY = "CONTENT_SECURITY_POLICY_BACKEND_REPORT_ONLY";

    private static final String DEFAULT_BACKEND_POLICY = "frame-ancestors 'self'; object-src 'none'";

    /**
     * Removes the environment variable and the property so each test starts from the shipped
     * configuration. The environment variable's Config entry is cleared first, because while it
     * exists {@link Config#setProperty} redirects the plain property name to it.
     */
    @After
    public void tearDown() {
        EnvironmentVariablesService.getInstance().getenv().remove(HSTS_ENV_VAR);
        Config.setProperty(HSTS_ENV_VAR, null);
        Config.setProperty(DotHttpHeaderSecurityFilter.HSTS_MAX_AGE_SECONDS, null);
        Config.setProperty(CSP_ENABLED, null);
        Config.setProperty(CSP_POLICY, null);
        Config.setProperty(CSP_REPORT_ONLY, null);
    }

    /**
     * Method to test: {@link DotHttpHeaderSecurityFilter#init}
     * Given scenario: no value is configured and the filter has the web.xml init-param it ships with
     * Expected result: the header carries a one year max-age and keeps includeSubDomains
     */
    @Test
    public void testInit_whenNothingConfigured_shouldSendOneYearMaxAge() throws Exception {
        Config.setProperty(DotHttpHeaderSecurityFilter.HSTS_MAX_AGE_SECONDS, null);

        assertEquals("max-age=31536000;includeSubDomains",
                hstsHeaderFor(Collections.singletonMap("hstsIncludeSubDomains", "true")));
    }

    /**
     * Method to test: {@link DotHttpHeaderSecurityFilter#init}
     * Given scenario: the max-age property is set to a custom value
     * Expected result: the header carries the configured value
     */
    @Test
    public void testInit_whenPropertySet_shouldSendConfiguredMaxAge() throws Exception {
        Config.setProperty(DotHttpHeaderSecurityFilter.HSTS_MAX_AGE_SECONDS, 600);

        assertEquals("max-age=600", hstsHeaderFor(Collections.emptyMap()));
    }

    /**
     * Method to test: {@link DotHttpHeaderSecurityFilter#init}
     * Given scenario: the property is set and web.xml also declares hstsMaxAgeSeconds
     * Expected result: the web.xml init-param wins
     */
    @Test
    public void testInit_whenInitParamDeclared_shouldPreferInitParam() throws Exception {
        Config.setProperty(DotHttpHeaderSecurityFilter.HSTS_MAX_AGE_SECONDS, 600);

        assertEquals("max-age=120",
                hstsHeaderFor(Collections.singletonMap("hstsMaxAgeSeconds", "120")));
    }

    /**
     * Method to test: {@link DotHttpHeaderSecurityFilter#init}
     * Given scenario: the property is not a number
     * Expected result: init does not throw and the header carries the one year default
     */
    @Test
    public void testInit_whenPropertyNotANumber_shouldFallBackToDefault() throws Exception {
        Config.setProperty(DotHttpHeaderSecurityFilter.HSTS_MAX_AGE_SECONDS, "one-year");

        assertEquals("max-age=31536000", hstsHeaderFor(Collections.emptyMap()));
    }

    /**
     * Method to test: {@link DotHttpHeaderSecurityFilter#init}
     * Given scenario: the property is negative
     * Expected result: the header carries the one year default rather than max-age=0
     */
    @Test
    public void testInit_whenPropertyNegative_shouldFallBackToDefault() throws Exception {
        Config.setProperty(DotHttpHeaderSecurityFilter.HSTS_MAX_AGE_SECONDS, -1);

        assertEquals("max-age=31536000", hstsHeaderFor(Collections.emptyMap()));
    }

    /**
     * Method to test: {@link DotHttpHeaderSecurityFilter#init}
     * Given scenario: the property is 0, which an operator may set to clear HSTS in browsers
     * Expected result: the value is kept and the header carries max-age=0
     */
    @Test
    public void testInit_whenPropertyZero_shouldSendZero() throws Exception {
        Config.setProperty(DotHttpHeaderSecurityFilter.HSTS_MAX_AGE_SECONDS, 0);

        assertEquals("max-age=0", hstsHeaderFor(Collections.emptyMap()));
    }

    /**
     * Method to test: {@link DotHttpHeaderSecurityFilter#init}
     * Given scenario: DOT_HSTS_MAX_AGE_SECONDS holds a usable value
     * Expected result: the header carries the environment variable's value
     */
    @Test
    public void testInit_whenEnvVarSet_shouldSendEnvVarMaxAge() throws Exception {
        setEnvVar("600");

        assertEquals("max-age=600", hstsHeaderFor(Collections.emptyMap()));
    }

    /**
     * Method to test: {@link DotHttpHeaderSecurityFilter#init}
     * Given scenario: DOT_HSTS_MAX_AGE_SECONDS is not a number while the property holds 600
     * Expected result: a warning is logged and the header carries the one year default, not the
     * property's value
     */
    @Test
    public void testInit_whenEnvVarNotANumber_shouldWarnAndFallBackToDefault() throws Exception {
        Config.setProperty(DotHttpHeaderSecurityFilter.HSTS_MAX_AGE_SECONDS, 600);
        setEnvVar("one-year");

        assertWarnsAndSendsDefault();
    }

    /**
     * Method to test: {@link DotHttpHeaderSecurityFilter#init}
     * Given scenario: DOT_HSTS_MAX_AGE_SECONDS is a whole number too large for an int while the
     * property holds 600
     * Expected result: a warning is logged and the header carries the one year default
     */
    @Test
    public void testInit_whenEnvVarTooLarge_shouldWarnAndFallBackToDefault() throws Exception {
        Config.setProperty(DotHttpHeaderSecurityFilter.HSTS_MAX_AGE_SECONDS, 600);
        setEnvVar("2147483648");

        assertWarnsAndSendsDefault();
    }

    /**
     * Sets DOT_HSTS_MAX_AGE_SECONDS the way {@code ConfigTest} does: through
     * {@link EnvironmentVariablesService}, followed by a Config reload so the variable is read in.
     */
    private static void setEnvVar(final String value) {
        EnvironmentVariablesService.getInstance().put(HSTS_ENV_VAR, value);
        Config.initializeConfig();
    }

    /**
     * Initializes the filter with no init-params and checks that it logged a warning and sent the
     * one year default.
     */
    private void assertWarnsAndSendsDefault() throws Exception {
        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            assertEquals("max-age=31536000", hstsHeaderFor(Collections.emptyMap()));
            logger.verify(() -> Logger.warn(any(DotHttpHeaderSecurityFilter.class), anyString()));
        }
    }

    /**
     * Initializes a filter with the given init-params, sends one secure request through it and
     * returns the Strict-Transport-Security value it wrote to the response.
     */
    private String hstsHeaderFor(final Map<String, String> initParams) throws Exception {
        final FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getInitParameterNames())
                .thenReturn(Collections.enumeration(initParams.keySet()));
        initParams.forEach((name, value) -> when(filterConfig.getInitParameter(name)).thenReturn(value));

        final DotHttpHeaderSecurityFilter filter = new DotHttpHeaderSecurityFilter();
        filter.init(filterConfig);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.isSecure()).thenReturn(true);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        filter.doFilter(request, response, mock(FilterChain.class));

        final ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq(HSTS_HEADER), value.capture());
        return value.getValue();
    }

    /**
     * Method to test: {@link DotHttpHeaderSecurityFilter#doFilter}
     * Given scenario: nothing configured; requests for the back-end UI, the REST API and the legacy
     * JSP pages, with the path split between servlet path and path info
     * the way the container maps each of them
     * Expected result: every response carries the default back-end policy
     */
    @Test
    public void testDoFilter_whenBackEndPath_shouldSendDefaultPolicy() throws Exception {
        assertEquals(DEFAULT_BACKEND_POLICY, cspHeaderFor("/dotAdmin", "/"));
        assertEquals(DEFAULT_BACKEND_POLICY, cspHeaderFor("/dotAdmin/index.html", null));
        assertEquals(DEFAULT_BACKEND_POLICY, cspHeaderFor("/api", "/v1/users/current"));
        assertEquals(DEFAULT_BACKEND_POLICY, cspHeaderFor("/html/portlet/ext/contentlet/view.jsp", null));
    }

    /**
     * Method to test: {@link DotHttpHeaderSecurityFilter#doFilter}
     * Given scenario: nothing configured; requests for site pages, assets and paths that only
     * share a leading string with a back-end prefix
     * Expected result: no policy header is written, so site pages keep their own configuration
     */
    @Test
    public void testDoFilter_whenSitePath_shouldNotSendPolicy() throws Exception {
        for (final String path : new String[]{"/", "/about-us/index", "/dotAdminX", "/application/x",
                "/contentAsset/raw-data/abc/fileAsset", "/dA/abc", "/cars", "/htmlpages/x"}) {
            final HttpServletResponse response = filter(path, null);
            verify(response, never()).setHeader(eq(CSP_HEADER), anyString());
            verify(response, never()).setHeader(eq(CSP_REPORT_ONLY_HEADER), anyString());
        }
    }

    /**
     * Method to test: {@link DotHttpHeaderSecurityFilter#doFilter}
     * Given scenario: nothing configured; requests mapped to the portal servlet ({@code /c}), which
     * may be a site page under a root folder named {@code c} that CMSFilter serves
     * Expected result: the filter writes no policy; the portal servlet writes it on its own
     * responses (see {@code MainServletTest})
     */
    @Test
    public void testDoFilter_whenPortalServletPath_shouldLeaveItToTheServlet() throws Exception {
        for (final String pathInfo : new String[]{null, "/", "/about/index", "/portal/layout"}) {
            final HttpServletResponse response = filter("/c", pathInfo);
            verify(response, never()).setHeader(eq(CSP_HEADER), anyString());
            verify(response, never()).setHeader(eq(CSP_REPORT_ONLY_HEADER), anyString());
        }
    }

    /**
     * Method to test: {@link DotHttpHeaderSecurityFilter#doFilter}
     * Given scenario: the back-end policy is switched off
     * Expected result: no policy header is written on a back-end path
     */
    @Test
    public void testDoFilter_whenDisabled_shouldNotSendPolicy() throws Exception {
        Config.setProperty(CSP_ENABLED, false);

        final HttpServletResponse response = filter("/dotAdmin", "/");
        verify(response, never()).setHeader(eq(CSP_HEADER), anyString());
        verify(response, never()).setHeader(eq(CSP_REPORT_ONLY_HEADER), anyString());
    }

    /**
     * Method to test: {@link DotHttpHeaderSecurityFilter#doFilter}
     * Given scenario: a custom policy is configured
     * Expected result: the custom policy replaces the default
     */
    @Test
    public void testDoFilter_whenPolicyConfigured_shouldSendConfiguredPolicy() throws Exception {
        Config.setProperty(CSP_POLICY, "frame-ancestors 'none'");

        assertEquals("frame-ancestors 'none'", cspHeaderFor("/api", "/v1/sites"));
    }

    /**
     * Method to test: {@link DotHttpHeaderSecurityFilter#doFilter}
     * Given scenario: the configured policy is blank
     * Expected result: no policy header is written on a back-end path
     */
    @Test
    public void testDoFilter_whenPolicyBlank_shouldNotSendPolicy() throws Exception {
        Config.setProperty(CSP_POLICY, "  ");

        final HttpServletResponse response = filter("/dotAdmin", "/");
        verify(response, never()).setHeader(eq(CSP_HEADER), anyString());
        verify(response, never()).setHeader(eq(CSP_REPORT_ONLY_HEADER), anyString());
    }

    /**
     * Method to test: {@link DotHttpHeaderSecurityFilter#doFilter}
     * Given scenario: report-only mode is switched on
     * Expected result: the policy goes out as Content-Security-Policy-Report-Only and the
     * enforcing header is not written
     */
    @Test
    public void testDoFilter_whenReportOnly_shouldSendReportOnlyHeader() throws Exception {
        Config.setProperty(CSP_REPORT_ONLY, true);

        final HttpServletResponse response = filter("/dotAdmin", "/");
        verify(response).setHeader(CSP_REPORT_ONLY_HEADER, DEFAULT_BACKEND_POLICY);
        verify(response, never()).setHeader(eq(CSP_HEADER), anyString());
    }

    /**
     * An unreadable value for the on/off switch must not fail the request: the default policy is
     * still sent.
     */
    @Test
    public void testDoFilter_whenEnabledValueUnreadable_shouldSendDefaultPolicy() throws Exception {
        Config.setProperty(CSP_ENABLED, "flase");

        final HttpServletResponse response = filter("/api", "/v1/sites");

        verify(response).setHeader(CSP_HEADER, DEFAULT_BACKEND_POLICY);
    }

    /**
     * An unreadable value for the report-only switch must not fail the request: the policy is
     * still enforced.
     */
    @Test
    public void testDoFilter_whenReportOnlyValueUnreadable_shouldEnforcePolicy() throws Exception {
        Config.setProperty(CSP_REPORT_ONLY, "maybe");

        final HttpServletResponse response = filter("/dotAdmin", null);

        verify(response).setHeader(CSP_HEADER, DEFAULT_BACKEND_POLICY);
        verify(response, never()).setHeader(eq(CSP_REPORT_ONLY_HEADER), anyString());
    }

    /**
     * Sends one request with the given servlet path and path info through a filter initialized
     * with no init-params and returns the Content-Security-Policy value it wrote.
     */
    private String cspHeaderFor(final String servletPath, final String pathInfo) throws Exception {
        final HttpServletResponse response = filter(servletPath, pathInfo);
        final ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq(CSP_HEADER), value.capture());
        return value.getValue();
    }

    /**
     * Sends one request with the given servlet path and path info through a filter initialized
     * with no init-params and returns the mocked response.
     */
    private HttpServletResponse filter(final String servletPath, final String pathInfo)
            throws Exception {
        final FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getInitParameterNames()).thenReturn(Collections.emptyEnumeration());

        final DotHttpHeaderSecurityFilter filter = new DotHttpHeaderSecurityFilter();
        filter.init(filterConfig);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn(servletPath);
        when(request.getPathInfo()).thenReturn(pathInfo);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        filter.doFilter(request, response, mock(FilterChain.class));
        return response;
    }

}
