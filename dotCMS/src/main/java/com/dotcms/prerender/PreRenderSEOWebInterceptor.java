package com.dotcms.prerender;

import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotmarketing.business.web.WebAPILocator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * The PreRender interceptor verifies if the request of the page is coming from a crawler bot (their spider)
 * if it is a robot request, instead of serving the page with the dotCMS engine, retrieves the page from thirdparty service (prerender.io)
 * that provides a SEO friendly version of the page for the crawler.
 * @author jsanca
 */
public class PreRenderSEOWebInterceptor implements WebInterceptor {

    private final PreRenderSEOWebAPI prerenderSeoWebAPI = WebAPILocator.getPreRenderSEOWebAPI();

    @Override
    public String[] getFilters() {
        return new String[] {"/*"};
    }

    @Override
    public Result intercept(final HttpServletRequest httpServletRequest,
                            final HttpServletResponse httpServletResponse) throws IOException {

        final boolean isPrerendered = this.prerenderIfEligible(
                httpServletRequest, httpServletResponse);
        if (!isPrerendered) {

            return Result.NEXT;
        }

        return Result.SKIP_NO_CHAIN;
    }

    private boolean prerenderIfEligible(final HttpServletRequest request, final HttpServletResponse response) {
        return this.prerenderSeoWebAPI.prerenderIfEligible(request, response);
    }
}
