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
 * ViewTool to inject the JS/HTML Analytics tracking code.
 * Usage in Velocity: $dotAnalytics.code()
 */
public class AnalyticsCodeInjecterViewTool implements ViewTool {

    final AnalyticsWebAPI analyticsWebAPI;
    final HostWebAPI hostWebAPI;
    final Supplier<HttpServletRequest> requestSupplier;

    public AnalyticsCodeInjecterViewTool() {
        this(WebAPILocator.getAnalyticsWebAPI(),
                WebAPILocator.getHostWebAPI(),
                HttpServletRequestThreadLocal.INSTANCE::getRequest);
    }

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
