package com.dotmarketing.filters;

import com.dotcms.repackage.com.google.common.base.CaseFormat;
import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.catalina.filters.HttpHeaderSecurityFilter;

/**
 * A wrapper class for {@link HttpHeaderSecurityFilter} that allows custom initialization parameters
 * through environment variables, providing flexibility in configuring HTTP header security settings.
 * <p>
 * This class implements the {@link Filter} interface, enabling it to intercept and modify request
 * and response objects in a web application's filter chain. Configuration parameters can be specified
 * via environment variables prefixed with {@value #CONFIG_PREFIX}, allowing for dynamic adjustment
 * of security header configurations without modifying web.xml.
 * </p>
 */
public class HttpHeaderSecurityFilterWrapper implements Filter {
    private final HttpHeaderSecurityFilter wrappedFilter = new HttpHeaderSecurityFilter();

    /**
     * The prefix used for environment variables that override filter initialization parameters.
     */
    private static final String CONFIG_PREFIX = "CMS_";

    /**
     * Initializes the wrapped {@link HttpHeaderSecurityFilter} with custom configuration parameters.
     * This method allows for initialization parameters to be provided via environment variables,
     * with a fallback to the traditional web.xml configuration.
     *
     * @param filterConfig the filter configuration object provided by the servlet container
     * @throws ServletException if an exception occurs that interrupts the filter's normal operation
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        FilterConfig customFilterConfig = new FilterConfig() {
            @Override
            public String getFilterName() {
                return filterConfig.getFilterName();
            }

            @Override
            public ServletContext getServletContext() {
                return filterConfig.getServletContext();
            }

            @Override
            public String getInitParameter(String name) {
                String paramValue = System.getenv(CONFIG_PREFIX + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name));
                if (paramValue != null) {
                    return paramValue;
                }
                return filterConfig.getInitParameter(name);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return filterConfig.getInitParameterNames();
            }
        };

        wrappedFilter.init(customFilterConfig);
    }

    /**
     * Delegates the request and response objects to the wrapped {@link HttpHeaderSecurityFilter}'s
     * {@code doFilter} method, allowing it to apply HTTP security headers as configured.
     *
     * @param request  the {@link ServletRequest} object that contains the client's request
     * @param response the {@link ServletResponse} object that contains the filter's response
     * @param chain    the {@link FilterChain} for invoking the next filter or the resource
     * @throws IOException      if an I/O error occurs during this filter's processing of the request
     * @throws ServletException if the processing fails for other reasons
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        wrappedFilter.doFilter(request, response, chain);
    }

    /**
     * Destroys the filter, allowing any resources held by the wrapped {@link HttpHeaderSecurityFilter}
     * to be released.
     */
    @Override
    public void destroy() {
        wrappedFilter.destroy();
    }
}