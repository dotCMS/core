package com.dotcms.cdn;

import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotmarketing.util.Logger;

public class CDNInterceptor implements WebInterceptor {

    private static final long serialVersionUID = 1L;

    @Override
    public String[] getFilters() {
        return new String[] {"/*"};
    }

    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) {

        final boolean recordDotHeaders = request.getParameter("recordDotHeaders") != null;
        if (recordDotHeaders) {
            final List<String> headers = Collections.list(request.getHeaderNames());
            for (final String header : headers) {
                Logger.info(this.getClass().getName(),
                        "  CDN : " + header + " : " + request.getHeader(header));
            }
        }

        return Result.NEXT;
    }
}
