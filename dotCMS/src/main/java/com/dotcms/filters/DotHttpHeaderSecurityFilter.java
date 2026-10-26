package com.dotcms.filters;

import com.dotmarketing.util.Config;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import org.apache.catalina.filters.HttpHeaderSecurityFilter;

/**
 * Tomcat's {@link HttpHeaderSecurityFilter} with the HSTS ({@code Strict-Transport-Security})
 * {@code max-age} read from dotCMS configuration instead of a fixed {@code web.xml} value.
 *
 * <p>The {@value #HSTS_MAX_AGE_SECONDS} property sets the {@code max-age} in seconds and defaults
 * to {@value #DEFAULT_HSTS_MAX_AGE_SECONDS} (one year). It can be set in a properties file or with
 * the {@code DOT_HSTS_MAX_AGE_SECONDS} environment variable, and is read once, when the filter is
 * initialized, so a change needs a restart. A {@code hstsMaxAgeSeconds} init-param declared on
 * this filter in {@code web.xml} still takes precedence over the property.
 *
 * <p>All other settings (such as {@code hstsIncludeSubDomains} and the X-Frame-Options value)
 * keep coming from the filter's {@code web.xml} init-params.
 */
public class DotHttpHeaderSecurityFilter extends HttpHeaderSecurityFilter {

    /** Config property holding the HSTS {@code max-age}, in seconds. */
    public static final String HSTS_MAX_AGE_SECONDS = "HSTS_MAX_AGE_SECONDS";

    /** One year, in seconds. */
    public static final int DEFAULT_HSTS_MAX_AGE_SECONDS = 31536000;

    /**
     * Applies the configured HSTS {@code max-age} and then lets Tomcat apply the init-params
     * declared in {@code web.xml}.
     *
     * @param filterConfig the filter configuration supplied by the servlet container
     * @throws ServletException if Tomcat rejects one of the init-params
     */
    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        setHstsMaxAgeSeconds(Config.getIntProperty(HSTS_MAX_AGE_SECONDS, DEFAULT_HSTS_MAX_AGE_SECONDS));
        super.init(filterConfig);
    }

}
