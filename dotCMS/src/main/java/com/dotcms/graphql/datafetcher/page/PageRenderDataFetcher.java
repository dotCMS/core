package com.dotcms.graphql.datafetcher.page;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContext;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContextBuilder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This DataFetcher returns a {@link String} containing the rendered HTML code of the requested page
 * requested {@link HTMLPageAsset}.
 */
public class PageRenderDataFetcher implements DataFetcher<String> {
    @Override
    public String get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final DotGraphQLContext context = environment.getContext();
            final User user = context.getUser();
            final HttpServletRequest request = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletRequest();
            final HttpServletResponse response = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletResponse();

            final String pageModeAsString = (String) context.getParam("pageMode");
            final String url = (String) context.getParam("url");
            Logger.debug(this, ()-> "Fetching page render: " + url);

            final PageMode mode = PageMode.get(pageModeAsString);

            final PageContext pageContext = PageContextBuilder.builder()
                    .setUser(user)
                    .setPageUri(url)
                    .setPageMode(mode)
                    .build();

            return APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(pageContext, request,
                        response);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
