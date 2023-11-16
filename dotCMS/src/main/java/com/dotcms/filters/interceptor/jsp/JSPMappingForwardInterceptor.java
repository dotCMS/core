package com.dotcms.filters.interceptor.forward;

import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotmarketing.util.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * This class maps an url to a jsp file into the WEB-INF folder
 */
public class JSPMappingForwardInterceptor implements WebInterceptor {

    private final Map<String, String> mappings = Map.of(
            "/custom-field-legacy", "/jsp/legacycustomfield/custom-field-legacy.jsp"
    );

    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) {

        final String path = request.getRequestURI();

        try {
            if (mappings.containsKey(path)) {

                final String newPath = "/WEB-INF"+mappings.get(path);
                request.getRequestDispatcher(newPath).forward(request, response);
                return Result.SKIP;
            }
        } catch (Exception e) {

            Logger.error(this.getClass(), e.getMessage(), e);
            throw new RuntimeException(e);
        }

        return Result.NEXT;
    }
}
