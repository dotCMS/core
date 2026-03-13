package com.dotcms.filters;

import com.dotmarketing.beans.Host;
import com.dotmarketing.util.WebKeys;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Request wrapper produced by {@link NestedHostResolutionFilter} when a
 * nested-host path prefix is stripped from the incoming URI.
 *
 * <p>Two overrides make the stripped URI transparent to all downstream code:
 * <ul>
 *   <li>{@link #getRequestURI()} — returns the stripped URI (e.g. {@code /about})
 *       instead of the original ({@code /en/nestedHost1/about}).</li>
 *   <li>{@link #getServletPath()} — likewise, so servlet path-matching is correct.</li>
 * </ul>
 *
 * <p>Additionally, {@link #getAttribute(String)} returns the pre-resolved nested
 * {@link Host} when queried for {@code WebKeys.CURRENT_HOST}, so that
 * {@code HostWebAPIImpl.getCurrentHost()} short-circuits without a second
 * cache or database hit.</p>
 */
class NestedHostRequestWrapper extends HttpServletRequestWrapper {

    private final String strippedUri;
    private final Host   resolvedHost;

    NestedHostRequestWrapper(final HttpServletRequest request,
                             final String strippedUri,
                             final Host   resolvedHost) {
        super(request);
        this.strippedUri  = strippedUri;
        this.resolvedHost = resolvedHost;
    }

    /** Returns the nested-host-prefix-stripped URI. */
    @Override
    public String getRequestURI() {
        return strippedUri;
    }

    /** Returns the nested-host-prefix-stripped servlet path. */
    @Override
    public String getServletPath() {
        return strippedUri;
    }

    /**
     * Pre-populates {@code WebKeys.CURRENT_HOST} so that
     * {@code HostWebAPIImpl.getCurrentHost()} returns the nested host object
     * immediately, without re-querying the cache or the database.
     */
    @Override
    public Object getAttribute(final String name) {
        if (WebKeys.CURRENT_HOST.equals(name)) {
            return resolvedHost;
        }
        return super.getAttribute(name);
    }
}
