package com.dotcms.filters;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;

/**
 * Filter created to wrap all the incoming requests to override the
 * {@link HttpServletRequest#getRequestURI()} method in order to normalize the requested URIs.
 */
public class NormalizationFilter implements Filter {


    /**
     * Allow a regex to be set in config that can filter for bad urls in case of future issues
     */
    final Lazy<Optional<Pattern>> forbiddenRegex = Lazy.of(() -> {
        final String pattern = Config.getStringProperty("URI_NORMALIZATION_FORBIDDEN_REGEX", null);
        return (pattern != null) ? Optional.of(Pattern.compile(pattern)) : Optional.empty();
    });


    /**
     * Default list of disallowed char sequences
     */
    final String[] DISALLOWED_URI_DEFAULT = new String[] {
            ";",
            "..",
            "/./",
            "\\",
            "//",
            "?",
            "%3B", // encoded semi-colon
            "%2E", // encoded period '.'
            "%5C", // encoded back slash '\'
            "%3F", // encoded questionmark
            "%00", // encoded null
            "\0",  // null
            "\r",  // carriage return
            "\n",  // line feed
            "\f"   // form feed
    };



    final Lazy<String[]> forbiddenURIStrings =
            Lazy.of(() -> Config.getStringArrayProperty("URI_NORMALIZATION_FORBIDDEN_STRINGS", DISALLOWED_URI_DEFAULT));




    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}


    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {


        final String originalUri = ((HttpServletRequest) servletRequest).getRequestURI();

        try {
            if (forbiddenRegex.get().isPresent() && forbiddenRegex.get().get().matcher(originalUri).find()) {
                throw new IllegalArgumentException("Invalid URI passed:" + originalUri);
            }
            if (containsNastyChar(originalUri)) {
                throw new IllegalArgumentException("Invalid URI passed:" + originalUri);
            }
        } catch (IllegalArgumentException iae) {
            Logger.warnAndDebug(getClass(),
                    "Invalid URI from:" + servletRequest.getRemoteAddr() + ", returning a 404", iae);
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {}

    /**
     * Normalization is the process of removing unnecessary "." and ".." segments from the path
     * component of a hierarchical URI. 1. Each "." segment is simply removed. 2. A ".." segment is
     * removed only if it is preceded by a non-".." segment. 3. Normalization has no effect upon opaque
     * URIs. (mailto:a@b.com)
     */

    boolean containsNastyChar(final String newNormal) {
        for (String reserved : forbiddenURIStrings.get()) {
            if (newNormal.contains(reserved)) {
                return true;
            }
        }
        return false;
    }



}