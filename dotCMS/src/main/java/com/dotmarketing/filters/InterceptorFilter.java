package com.dotmarketing.filters;

import com.dotcms.analytics.track.AnalyticsTrackWebInterceptor;
import com.dotcms.business.SystemTableUpdatedKeyEvent;
import com.dotcms.ema.EMAWebInterceptor;
import com.dotcms.filters.interceptor.AbstractWebInterceptorSupportFilter;
import com.dotcms.filters.interceptor.WebInterceptorDelegate;
import com.dotcms.filters.interceptor.meta.ResponseMetaDataWebInterceptor;
import com.dotcms.graphql.GraphqlCacheWebInterceptor;
import com.dotcms.jitsu.EventLogWebInterceptor;
import com.dotcms.prerender.PreRenderSEOWebInterceptor;
import com.dotcms.security.multipart.MultiPartRequestSecurityWebInterceptor;
import com.dotcms.variant.business.web.CurrentVariantWebInterceptor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

/**
 * This empty filter is useful to attach {@link com.dotcms.filters.interceptor.WebInterceptor}
 * objects to it. This is the first one in the filter pipeline and maps everything. This way, it's
 * not necessary to modify the web.xml file to add any new interceptors, and they can even be added
 * programmatically via OSGi plug-ins.
 *
 * @author jsanca
 */
public class InterceptorFilter extends AbstractWebInterceptorSupportFilter {

    @Override
    public void init(final FilterConfig config) throws ServletException {

        if (!Config.isSystemTableConfigSourceInit()) {
            Config.initSystemTableConfigSource();
        }
        this.addInterceptors(config);
        super.init(config);
    } // init.

    /**
     * Adds the interceptors to the delegate. You can add more to the list, as required.
     *
     * @param config The current instance of the {@link FilterConfig} object.
     */
    private void addInterceptors(final FilterConfig config) {
        final WebInterceptorDelegate delegate =
                this.getDelegate(config.getServletContext());

        final AnalyticsTrackWebInterceptor analyticsTrackWebInterceptor = new AnalyticsTrackWebInterceptor();
        // Note: RequestTrackingInterceptor replaced by RequestTrackingFilter for better performance and accuracy
        delegate.add(new MultiPartRequestSecurityWebInterceptor());
        delegate.add(new PreRenderSEOWebInterceptor());
        delegate.add(new EMAWebInterceptor());
        delegate.add(new GraphqlCacheWebInterceptor());
        delegate.add(new ResponseMetaDataWebInterceptor());
        delegate.add(new EventLogWebInterceptor());
        delegate.add(new CurrentVariantWebInterceptor());
        delegate.add(analyticsTrackWebInterceptor);
        APILocator.getLocalSystemEventsAPI().subscribe(SystemTableUpdatedKeyEvent.class, analyticsTrackWebInterceptor);
    } // addInterceptors.

} // E:O:F:InterceptorFilter.
