package com.dotcms.filters.interceptor.dotcms;

import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.shutdown.ShutdownCoordinator;
import com.dotmarketing.util.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This interceptor tracks active HTTP requests to support graceful shutdown.
 *
 * @author dotCMS Team
 * @version 5.4.0
 * @since 11-22-2023
 */
public class RequestTrackingInterceptor implements WebInterceptor {

    private static final String REQUEST_COUNTED_FLAG = "request-counted-flag";

    @Override
    public String getName() {
        return "RequestTrackingInterceptor";
    }

    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            ShutdownCoordinator.incrementActiveRequests();
            request.setAttribute(REQUEST_COUNTED_FLAG, true);
        } catch (Exception e) {
            Logger.debug(this, "Failed to increment request count, continuing without tracking: " + e.getMessage());
        }
        return Result.NEXT;
    }

    @Override
    public boolean afterIntercept(final HttpServletRequest request, final HttpServletResponse response) {
        if (request.getAttribute(REQUEST_COUNTED_FLAG) != null && (Boolean) request.getAttribute(REQUEST_COUNTED_FLAG)) {
            try {
                ShutdownCoordinator.decrementActiveRequests();
            } catch (Exception e) {
                Logger.debug(this, "Failed to decrement request count: " + e.getMessage());
            }
        }
        return true;
    }

    @Override
    public void destroy() {
        Logger.info(this, "RequestTrackingInterceptor destroyed");
    }

    @Override
    public void init() {
        Logger.info(this, "RequestTrackingInterceptor initialized - active request tracking enabled for graceful shutdown");
    }
}