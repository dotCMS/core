package com.dotcms.graphql.datafetcher.page;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.rendering.velocity.services.PageRenderUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import graphql.schema.DataFetchingEnvironment;

import java.util.Collections;
import java.util.List;

/**
 * This DataFetcher returns the {@link TemplateLayout} associated to the requested {@link HTMLPageAsset}.
 */
public class ContainersDataFetcher extends RedirectAwareDataFetcher<List<ContainerRaw>> {
    @Override
    public List<ContainerRaw> safeGet(final DataFetchingEnvironment environment, final DotGraphQLContext context) throws Exception {
        try {
            final User user = context.getUser();
            final Contentlet page = environment.getSource();
            Logger.debug(this, ()-> "Fetching containers for page: " + page.getIdentifier());
            final String pageModeAsString = (String) context.getParam("pageMode");
            final String languageId = (String) context.getParam("languageId");

            final PageMode mode = PageMode.get(pageModeAsString);
            final HTMLPageAsset pageAsset = APILocator.getHTMLPageAssetAPI()
                    .fromContentlet(page);

            final Host site = APILocator.getHostAPI().find(page.getHost(), user, true);
            PageRenderUtil pageRenderUtil = new PageRenderUtil(pageAsset, user, mode,
                    Long.parseLong(languageId), site);

            context.addParam("pageRenderUtil", pageRenderUtil);

            return pageRenderUtil.getContainersRaw();
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
    @Override
    protected List<ContainerRaw> onRedirect() {
        return Collections.emptyList();
    }
}
