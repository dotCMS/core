package com.dotcms.filters;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
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
 * {@code DOT_HSTS_MAX_AGE_SECONDS} environment variable) and an explicit {@code web.xml} init-param.
 */
public class DotHttpHeaderSecurityFilterTest extends UnitTestBase {

    private static final String HSTS_HEADER = "Strict-Transport-Security";

    private static final String HSTS_ENV_VAR = "DOT_HSTS_MAX_AGE_SECONDS";

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

}
