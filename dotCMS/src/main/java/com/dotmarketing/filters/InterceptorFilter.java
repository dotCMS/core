package com.dotmarketing.filters;

import com.dotcms.filters.interceptor.AbstractWebInterceptorSupportFilter;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

/**
 * This empty filter is useful to attach {@link com.dotcms.filters.interceptor.WebInterceptor}, it is the first one on the
 * filter pipeline and maps everything.
 * @author jsanca
 */
public class InterceptorFilter extends AbstractWebInterceptorSupportFilter {

    @Override
    public void init(final FilterConfig config) throws ServletException {

        super.init(config);
    } // init.

} // E:O:F:InterceptorFilter.