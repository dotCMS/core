package com.dotcms.filters.angular;

import com.dotcms.filters.interceptor.AbstractWebInterceptorSupportFilter;
import com.dotcms.filters.interceptor.WebInterceptorDelegate;
import com.dotcms.filters.interceptor.AngularRoutingDefaultInterceptor;

import javax.servlet.*;

/**
 * If the request is a Angular routing request then it response with a 304 Http code.
 */
public class AngularRoutingFilter extends AbstractWebInterceptorSupportFilter {

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        this.addDefaultInterceptors(filterConfig);
        super.init(filterConfig);
    }

    private void addDefaultInterceptors(final FilterConfig config) {
        final WebInterceptorDelegate delegate = this.getDelegate(config.getServletContext());
        delegate.add( new AngularRoutingDefaultInterceptor() );
    }
}
