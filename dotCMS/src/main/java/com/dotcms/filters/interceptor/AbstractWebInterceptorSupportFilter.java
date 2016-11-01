package com.dotcms.filters.interceptor;

import com.dotmarketing.util.Config;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Just encapsulates the Base logic to support interceptors in a filter.
 *
 * @author jsanca
 */
public abstract class AbstractWebInterceptorSupportFilter implements Filter {

    @Override
    public void init(final FilterConfig config) throws ServletException {

        this.getDelegate(config.getServletContext()).init();
    }

    @Override
    public void destroy() {

        this.getDelegate(Config.CONTEXT).destroy();
    } // destroy.

    /**
     * Runs all interceptors associated to this filter.
     * @param req {@link HttpServletRequest}
     * @param res {@link HttpServletResponse}
     * @return boolean true if all interceptors ran successfully.
     * @throws IOException
     */
    protected boolean runInterceptors (final HttpServletRequest req, HttpServletResponse res) throws IOException {

        return this.getDelegate(req).intercept(req, res);
    } // runInterceptor.


    protected WebInterceptorDelegate getDelegate (final HttpServletRequest req) {

        final FilterWebInterceptorProvider filterWebInterceptorProvider =
                FilterWebInterceptorProvider.getInstance(req);

        final WebInterceptorDelegate delegate =
                filterWebInterceptorProvider.getDelegate(this.getClass());

        return delegate;
    } // getDelegate.

    protected WebInterceptorDelegate getDelegate (final ServletContext context) {

        final FilterWebInterceptorProvider filterWebInterceptorProvider =
                FilterWebInterceptorProvider.getInstance(context);

        final WebInterceptorDelegate delegate =
                filterWebInterceptorProvider.getDelegate(this.getClass());

        return delegate;
    } // getDelegate.

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
            ServletException {

        final HttpServletResponse response = (HttpServletResponse) res;
        final HttpServletRequest request   = (HttpServletRequest) req;

        if (this.runInterceptors(request, response)) {

            chain.doFilter(req, response);
        }
    } // doFilter.

} // E:O:F:AbstractWebInterceptorSupportFilter.
