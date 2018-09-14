/*
 * WebSessionFilter
 * 
 * A filter that recognizes return users who have chosen to have their login
 * information remembered. Creates a valid WebSession object and passes it a
 * contact to use to fill its information
 *  
 */
package com.dotmarketing.filters;

import com.dotcms.filters.interceptor.AbstractWebInterceptorSupportFilter;
import com.dotcms.filters.interceptor.WebInterceptorDelegate;
import com.dotcms.filters.interceptor.dotcms.DefaultBackEndLoginRequiredWebInterceptor;
import com.dotcms.filters.interceptor.dotcms.DefaultFrontEndLoginRequiredWebInterceptor;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

/**
 * This Filter is in charge if checking if the user is logged in or not.
 * By default will use an interceptor that check if the user is logged in, if it is not, will returns a 401 and set a REDIRECT_AFTER_LOGIN
 *
 * In addition you can extends the intercept functionality by implementing your on filter.
 * @author jsanca
 */
public class LoginRequiredFilter extends AbstractWebInterceptorSupportFilter {

    @Override
    public void init(final FilterConfig config) throws ServletException {

        this.addDefaultInterceptors (config);
        super.init(config);
    } // init.

    // add the previous legacy code to be align with the interceptor approach.
    private void addDefaultInterceptors(final FilterConfig config) {

        final WebInterceptorDelegate delegate =
                this.getDelegate(config.getServletContext());

        delegate.add(new DefaultFrontEndLoginRequiredWebInterceptor());
        delegate.add(new DefaultBackEndLoginRequiredWebInterceptor());

    } // addDefaultInterceptors.

} // LoginRequiredFilter.