/*
 * WebSessionFilter
 *
 * A filter that recognizes return users who have
 * chosen to have their login information remembered.
 * Creates a valid WebSession object and
 * passes it a contact to use to fill its information
 *
 */
package com.dotmarketing.filters;

import com.dotcms.cms.login.LogoutWebInterceptor;
import com.dotcms.filters.interceptor.AbstractWebInterceptorSupportFilter;
import com.dotcms.filters.interceptor.WebInterceptorDelegate;
import com.dotcms.filters.interceptor.dotcms.DefaultAutoLoginWebInterceptor;
import com.dotcms.filters.interceptor.saml.SamlWebInterceptor;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

/**
 * Auto login is useful to do the auto login based on some configuration and preconditions.
 * DotCMS offers several approaches to do auto login, for instance CAS, JWT, OpenSAML, etc.
 * @author jsanca
 */
public class AutoLoginFilter extends AbstractWebInterceptorSupportFilter {

    @Override
    public void init(final FilterConfig config) throws ServletException {

        this.addDefaultInterceptors (config);
        super.init(config);
    } // init.

    // add the previous legacy code to be align with the interceptor approach.
    private void addDefaultInterceptors(final FilterConfig config) {

        final WebInterceptorDelegate delegate =
            this.getDelegate(config.getServletContext());

        delegate.add(new LogoutWebInterceptor());
        delegate.add(new DefaultAutoLoginWebInterceptor());
    } // addDefaultInterceptors.


} // E:O:F:AutoLoginFilter.
