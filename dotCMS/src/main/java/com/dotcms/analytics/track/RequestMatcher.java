package com.dotcms.analytics.track;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.RegEX;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Objects;
import java.util.Set;

/**
 * Matcher to include the tracking for analytics of some request.
 *
 * @author jsanca
 */
public interface RequestMatcher {

    String CHARSET = Config.getStringProperty("CHARSET", "UTF-8");

    /**
     * Return true if match the request with the patterns and methods
     * @param request {@link HttpServletRequest}
     * @return boolean true if the request match the patterns and methods
     */
    default boolean match(final HttpServletRequest request) {

        final Set<String> patterns = getMatcherPatterns();
        final Set<String> methods  = getAllowedMethods();
        return  Objects.nonNull(patterns) && !patterns.isEmpty() &&
                Objects.nonNull(methods) &&  !methods.isEmpty()  &&
                isAllowedMethod (methods, request.getMethod())   &&
                match(request, patterns);
    }

    /**
     * Match the request with the patterns
     * @param request {@link HttpServletRequest}
     * @param patterns Set of patterns
     * @return boolean true if any of the patterns match the request
     */
    default boolean match(final HttpServletRequest request, final Set<String> patterns) {

        final String requestURI = request.getRequestURI();
        return patterns.stream().anyMatch(pattern -> match(requestURI, pattern));
    }

    /**
     * Match the request URI with the pattern
     * @param requestURI String
     * @param pattern String
     * @return boolean true if the pattern match the request URI
     */
    default boolean match (final String requestURI, final String pattern) {

        String uftUri = null;

        try {

            uftUri = URLDecoder.decode(requestURI, CHARSET);
        } catch (UnsupportedEncodingException e) {

            uftUri = requestURI;
        }

        return RegEX.containsCaseInsensitive(uftUri, pattern.trim());
    } // match.

    /**
     * Determinate if the method is allowed
     * @param methods Set of methods
     * @param method String current request method
     * @return boolean true if the method is allowed
     */
    default boolean isAllowedMethod(final Set<String> methods, final String method) {

            return methods.contains(method);
    }

    /**
     * Returns a set of patterns for the matcher
     * @return Set by default empty
     */
    default Set<String> getMatcherPatterns() {

        return Set.of();
    }

    /**
     * Returns the request methods allowed for this matcher.
     * @return Set by default empty
     */
    default Set<String> getAllowedMethods() {

        return Set.of();
    }

    /**
     * Return an id for the Matcher, by default returns the class name.
     * @return
     */
    default String getId() {

        return this.getClass().getName();
    }
}
