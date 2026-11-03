package com.dotcms.filters;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
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
 * initialized, so a change needs a restart. A value that is not a whole number (including one too
 * large for an {@code int}), or is negative, is logged as a warning and replaced by the default so
 * a bad setting cannot stop the web application from starting. This holds for every source the
 * value can come from: the environment variable, a properties file, the system table or
 * {@link Config#setProperty}. A value of {@code 0} is kept and logged: it tells browsers to forget
 * HSTS for the host. A {@code hstsMaxAgeSeconds} init-param declared on
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
        setHstsMaxAgeSeconds(resolveMaxAgeSeconds());
        super.init(filterConfig);
    }

    /**
     * Reads the configured {@code max-age}. A value that cannot be read as an integer or is
     * below zero (which Tomcat would silently turn into {@code max-age=0}) falls back to the
     * default with a warning, and {@code 0}, which clears HSTS in browsers, is accepted but logged.
     *
     * <p>The value is read as a string and parsed here rather than through
     * {@link Config#getIntProperty(String, int)}, because that method quietly skips an
     * environment variable it cannot parse and returns the properties-file value instead, so the
     * warning would never fire for {@code DOT_HSTS_MAX_AGE_SECONDS}.
     *
     * @return the {@code max-age} in seconds to send
     */
    private int resolveMaxAgeSeconds() {
        final String configured = Config.getStringProperty(HSTS_MAX_AGE_SECONDS,
                String.valueOf(DEFAULT_HSTS_MAX_AGE_SECONDS));
        final int maxAge;
        try {
            maxAge = Integer.parseInt(configured.trim());
        } catch (NumberFormatException e) {
            Logger.warn(this, HSTS_MAX_AGE_SECONDS + " is not a whole number; using the default of "
                    + DEFAULT_HSTS_MAX_AGE_SECONDS + " seconds: " + e.getMessage());
            return DEFAULT_HSTS_MAX_AGE_SECONDS;
        }
        if (maxAge < 0) {
            Logger.warn(this, HSTS_MAX_AGE_SECONDS + " is " + maxAge + ", which is below 0; using the"
                    + " default of " + DEFAULT_HSTS_MAX_AGE_SECONDS + " seconds");
            return DEFAULT_HSTS_MAX_AGE_SECONDS;
        }
        if (maxAge == 0) {
            Logger.info(this, HSTS_MAX_AGE_SECONDS + " is 0: browsers that receive the"
                    + " Strict-Transport-Security header will stop treating this host as HTTPS-only");
        }
        return maxAge;
    }

}
