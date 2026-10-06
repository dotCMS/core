package com.dotcms.analytics.viewtool;

import com.dotcms.analytics.web.AnalyticsWebAPI;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.util.StringPool;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.tools.view.tools.ViewTool;

/**
 * Velocity ViewTool that exposes manual analytics script injection to template authors via
 * {@code $dotAnalytics.code()}.
 *
 * <p>dotCMS already injects the analytics {@code <script>} tag automatically through
 * {@code HTMLPageAssetRenderedAPIImpl}. However, that mechanism gives template authors no control
 * over placement, and it injects unconditionally — including on pages that return non-HTML formats
 * (JSON, XML, plain text), which corrupts those responses. This ViewTool gives template authors an
 * explicit opt-in alternative: call {@code $!dotAnalytics.code()} at the exact location in the
 * template where the script should appear.
 *
 * <p>This mirrors {@code ExperimentCodeInjecterViewTool} ({@code $dotExperiment.code()}), which
 * provides the same capability for the A/B Experiments feature.
 *
 * <p><b>Usage in Velocity templates:</b>
 * <pre>{@code
 * $!dotAnalytics.code()
 * }</pre>
 *
 * <p><b>Return value rules:</b>
 * <ul>
 *   <li>Returns the analytics {@code <script>} tag when the page is served in <em>LIVE</em> mode
 *       and an analytics app is configured for the current host.</li>
 *   <li>Returns an empty string in preview or edit mode, when no analytics app is configured,
 *       or when the auto-injection flag is disabled.</li>
 * </ul>
 *
 * <p><b>Interaction with auto-injection:</b> This ViewTool and the automatic injection mechanism
 * are independent. Using this ViewTool does <em>not</em> disable auto-injection. Template authors
 * who switch to manual injection are responsible for turning off the auto-injection flag to avoid
 * double injection.
 */
public class AnalyticsCodeInjecterViewTool implements ViewTool {

    final AnalyticsWebAPI analyticsWebAPI;
    final HostWebAPI hostWebAPI;
    final Supplier<HttpServletRequest> requestSupplier;

    /**
     * Production constructor. Dependencies are resolved from {@link WebAPILocator} and
     * {@link HttpServletRequestThreadLocal}.
     */
    public AnalyticsCodeInjecterViewTool() {
        this(WebAPILocator.getAnalyticsWebAPI(),
                WebAPILocator.getHostWebAPI(),
                HttpServletRequestThreadLocal.INSTANCE::getRequest);
    }

    /**
     * Testing constructor. Allows injecting mock dependencies without a running dotCMS context.
     *
     * @param analyticsWebAPI  provides the analytics script code
     * @param hostWebAPI       resolves the current host from the request
     * @param requestSupplier  supplies the current {@link HttpServletRequest}
     */
    AnalyticsCodeInjecterViewTool(final AnalyticsWebAPI analyticsWebAPI,
            final HostWebAPI hostWebAPI,
            final Supplier<HttpServletRequest> requestSupplier) {
        this.analyticsWebAPI = analyticsWebAPI;
        this.hostWebAPI = hostWebAPI;
        this.requestSupplier = requestSupplier;
    }

    @Override
    public void init(Object initData) {
    }

    /**
     * Returns the analytics {@code <script>} tag for the current host and request, or an empty
     * string when analytics output is not applicable.
     *
     * <p>Delegates to {@link AnalyticsWebAPI#getCode(Host, HttpServletRequest)}, which handles
     * all conditional logic (LIVE mode check, app configuration, flag state).
     *
     * @return the analytics script HTML, or {@link com.liferay.util.StringPool#BLANK} when no
     *         output should be rendered
     * @throws DotRuntimeException if the current host or security context cannot be resolved
     */
    public String code() {
        try {
            final Host currentHost = hostWebAPI.getCurrentHost();
            final HttpServletRequest request = requestSupplier.get();

            return analyticsWebAPI.getCode(currentHost, request).orElse(StringPool.BLANK);

        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }
}
