package com.liferay.portal.servlet.angular;

import com.dotcms.filters.interceptor.AbstractWebInterceptorSupportFilter;
import com.dotcms.filters.interceptor.WebInterceptorDelegate;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.util.Config;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

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
