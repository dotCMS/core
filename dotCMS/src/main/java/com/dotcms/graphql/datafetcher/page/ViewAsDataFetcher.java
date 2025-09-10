package com.dotcms.graphql.datafetcher.page;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.ViewAsPageStatus;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import graphql.schema.DataFetchingEnvironment;

import javax.servlet.http.HttpServletRequest;

/**
 * This DataFetcher returns a {@link ViewAsPageStatus} representing the view as status of the originally
 */
public class ViewAsDataFetcher extends RedirectAwareDataFetcher<ViewAsPageStatus> {
    @Override
    public ViewAsPageStatus safeGet(final DataFetchingEnvironment environment, final DotGraphQLContext context) throws Exception {
        try {
            final User user = context.getUser();
            final Contentlet contentlet = environment.getSource();
            final String pageModeAsString = (String) context.getParam("pageMode");

            final PageMode mode = PageMode.get(pageModeAsString);

            final HttpServletRequest request = context
                    .getHttpServletRequest();

            final HTMLPageAsset pageAsset = APILocator.getHTMLPageAssetAPI()
                    .fromContentlet(contentlet);

            return APILocator.getHTMLPageAssetRenderedAPI().getViewAsStatus(request,
                    mode, pageAsset, user);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    protected ViewAsPageStatus onRedirect() {
        return null;
    }


}
