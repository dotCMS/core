package com.dotcms.filters;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotmarketing.util.Config;
import java.util.Collections;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Checks the {@code Strict-Transport-Security} header written by {@link DotHttpHeaderSecurityFilter}
 * for the default, a configured value and an explicit {@code web.xml} init-param.
 */
public class DotHttpHeaderSecurityFilterTest extends UnitTestBase {

    private static final String HSTS_HEADER = "Strict-Transport-Security";

    @After
    public void tearDown() {
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
