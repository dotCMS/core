package com.dotcms.graphql.datafetcher.page;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.rendering.velocity.services.PageRenderUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import graphql.schema.DataFetchingEnvironment;

/**
 * Returns the total number of contentlets placed across all containers in the requested page.
 * This mirrors the {@code numberContents} field exposed by the REST Page API.
 */
public class NumberContentsDataFetcher extends RedirectAwareDataFetcher<Integer> {

    @Override
    public Integer safeGet(final DataFetchingEnvironment environment, final DotGraphQLContext context) throws Exception {
        try {
            final Contentlet page = environment.getSource();
            Logger.debug(this, () -> "Fetching numberContents for page: " + page.getIdentifier());

            PageRenderUtil pageRenderUtil = (PageRenderUtil) context.getParam("pageRenderUtil");

            if (pageRenderUtil == null) {
                final User user = context.getUser();
                final String pageModeAsString = (String) context.getParam("pageMode");
                final String languageId = (String) context.getParam("languageId");
                final PageMode mode = PageMode.get(pageModeAsString);
                final HTMLPageAsset pageAsset = APILocator.getHTMLPageAssetAPI().fromContentlet(page);
                final Host site = APILocator.getHostAPI().find(page.getHost(), user, true);
                pageRenderUtil = new PageRenderUtil(pageAsset, user, mode, Long.parseLong(languageId), site);
            }

            return pageRenderUtil.getContainersRaw().stream()
                    .flatMap(containerRaw -> containerRaw.getContentlets().values().stream())
                    .mapToInt(java.util.List::size)
                    .sum();
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    protected Integer onRedirect() {
        return 0;
    }
}
