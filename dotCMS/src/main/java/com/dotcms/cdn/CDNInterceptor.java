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

    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) {

        final boolean recordDotHeaders = request.getParameter("recordDotHeaders") != null
                && Config.getBooleanProperty("DOT_CDN_DEBUG_HEADERS", false);
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
