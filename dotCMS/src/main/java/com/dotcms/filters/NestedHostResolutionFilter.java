package com.dotcms.filters;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostResolutionResult;
import com.dotmarketing.portlets.contentlet.business.NestedHostPatternCache;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Servlet filter that performs nested-host path-segment resolution before
 * the request reaches {@link com.dotmarketing.filters.CMSFilter}.
 *
 * <p><strong>What it does</strong><br>
 * For a request like {@code GET https://tophost.com/en/nestedHost1/about}:
 * <ol>
 *   <li>Looks up the top-level {@link Host} for {@code tophost.com}.</li>
 *   <li>Calls {@link NestedHostPatternCache#resolve(String, String)} to test
 *       whether any registered nested host matches the leading path segments.</li>
 *   <li>On a match, wraps the request in a {@link NestedHostRequestWrapper} that:
 *       overrides {@code getRequestURI()} / {@code getServletPath()} to return
 *       the stripped URI ({@code /about}); and pre-populates
 *       {@code WebKeys.CURRENT_HOST} so downstream code returns the nested host
 *       without re-querying the cache or database.</li>
 *   <li>Passes the (possibly wrapped) request down the filter chain.</li>
 * </ol>
 *
 * <p><strong>Filter ordering (web.xml)</strong><br>
 * Declared after {@code VanityURLFilter} and before {@code CMSFilter}.
 * Every downstream filter and servlet will see the stripped URI and the
 * pre-resolved host with no further changes needed.</p>
 *
 * <p><strong>Performance</strong><br>
 * Non-nested requests incur only one {@link NestedHostPatternCache} lookup
 * (JVM-local) and one {@code HostAPI.findByName} call (also cached).
 * The wrapper object is only allocated on actual nested-host hits.</p>
 */
public class NestedHostResolutionFilter implements Filter {

    @Override
    public void init(final FilterConfig filterConfig) {
        // NestedHostPatternCache manages its own lifecycle.
    }

    @Override
    public void doFilter(final ServletRequest req,
                         final ServletResponse res,
                         final FilterChain chain)
            throws IOException, ServletException {

        if (!(req instanceof HttpServletRequest)) {
            chain.doFilter(req, res);
            return;
        }

        final HttpServletRequest request    = (HttpServletRequest) req;
        final String             serverName = request.getServerName();
        final String             fullUri    = request.getRequestURI();

        try {
            final User systemUser = APILocator.getUserAPI().getSystemUser();
            final Host topLevelHost = APILocator.getHostAPI()
                    .findByName(serverName, systemUser, false);

            if (topLevelHost != null && !topLevelHost.isSystemHost()) {
                final HostResolutionResult result =
                        NestedHostPatternCache.getInstance()
                                .resolve(topLevelHost.getIdentifier(), fullUri);

                if (result.isNested()) {
                    // Load the nested Host object from the resolved ID.
                    final Host nestedHost = APILocator.getHostAPI()
                            .find(result.getResolvedHostId(), systemUser, false);

                    if (nestedHost != null) {
                        Logger.debug(this,
                                () -> String.format(
                                        "[NestedHostResolutionFilter] %s%s -> host=%s uri=%s",
                                        serverName, fullUri,
                                        nestedHost.getHostname(),
                                        result.getRemainingUri()));

                        final NestedHostRequestWrapper wrapper =
                                new NestedHostRequestWrapper(
                                        request,
                                        result.getRemainingUri(),
                                        nestedHost);
                        // AC9: signal to downstream consumers which host was resolved
                        wrapper.setAttribute(WebKeys.CMS_RESOLVED_HOST, nestedHost);
                        chain.doFilter(wrapper, res);
                        return;
                    }
                }
            }
        } catch (final Exception e) {
            // Never block a request due to nested-host resolution failure.
            Logger.warnAndDebug(NestedHostResolutionFilter.class,
                    "Nested host resolution failed for " + serverName + fullUri
                            + ": " + e.getMessage(), e);
        }

        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {
        // Nothing to tear down.
    }
}
