package com.dotmarketing.filters;

import com.dotcms.ema.EMAWebInterceptor;
import com.dotcms.filters.interceptor.AbstractWebInterceptorSupportFilter;
import com.dotcms.filters.interceptor.WebInterceptorDelegate;
import com.dotcms.filters.interceptor.meta.ResponseMetaDataWebInterceptor;
import com.dotcms.graphql.GraphqlCacheWebInterceptor;
import com.dotcms.jitsu.EventLogWebInterceptor;
import com.dotcms.security.multipart.MultiPartRequestSecurityWebInterceptor;
import com.dotcms.prerender.PreRenderSEOWebInterceptor;

import com.dotcms.variant.business.web.CurrentVariantWebInterceptor;
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

        this.addInterceptors(config);
        super.init(config);
    } // init.

    private void addInterceptors(final FilterConfig config) {

        final WebInterceptorDelegate delegate =
                this.getDelegate(config.getServletContext());

        delegate.add(new MultiPartRequestSecurityWebInterceptor());
        delegate.add(new PreRenderSEOWebInterceptor());
        delegate.add(new EMAWebInterceptor());
        delegate.add(new GraphqlCacheWebInterceptor());
        delegate.add(new ResponseMetaDataWebInterceptor());
        delegate.add(new EventLogWebInterceptor());
        delegate.add(new CurrentVariantWebInterceptor());
    } // addInterceptors.

} // E:O:F:InterceptorFilter.
