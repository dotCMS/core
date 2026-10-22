package com.dotcms.cdn;

import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

public class CDNInterceptor implements WebInterceptor {

    private static final long serialVersionUID = 1L;
    private static final List<String> SENSITIVE_HEADERS = List.of(
            "authorization", "cookie", "proxy-authorization", "set-cookie", "x-csrf-token",
            "x-xsrf-token");

    @Override
    public String[] getFilters() {
        return new String[] {"/*"};
    }

    /**
     * Logs the request headers, with sensitive ones redacted, when the {@code DOT_CDN_DEBUG_HEADERS}
     * property is on and the request carries a {@code recordDotHeaders} parameter. This helps check
     * what a CDN forwards to dotCMS. It never stops or alters the request.
     *
     * <p>The property is checked first on purpose. This interceptor sees every request, and reading
     * a parameter on a form-encoded POST makes the container consume the request body, so the
     * parameter must not be touched unless the debug switch is on.
     *
     * @param request the incoming {@link HttpServletRequest}
     * @param response the {@link HttpServletResponse}, not used
     * @return always {@link Result#NEXT}
     */
    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) {

        final boolean recordDotHeaders = Config.getBooleanProperty("DOT_CDN_DEBUG_HEADERS", false)
                && request.getParameter("recordDotHeaders") != null;
        if (recordDotHeaders) {
            final List<String> headers = Collections.list(request.getHeaderNames());
            for (final String header : headers) {
                Logger.info(this.getClass().getName(),
                        "  CDN : " + header + " : " + headerValue(request, header));
            }
        }

        return Result.NEXT;
    }

    private String headerValue(final HttpServletRequest request, final String header) {
        return SENSITIVE_HEADERS.contains(header.toLowerCase()) ? "[REDACTED]" : request.getHeader(header);
    }
}
