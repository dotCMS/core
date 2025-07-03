package com.dotcms.graphql.datafetcher.page;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import graphql.schema.DataFetchingEnvironment;

/**
 * This DataFetcher returns the {@link TemplateLayout} associated to the requested {@link HTMLPageAsset}.
 */
public class LayoutDataFetcher extends RedirectAwareDataFetcher<TemplateLayout> {
    @Override
    public TemplateLayout safeGet(final DataFetchingEnvironment environment, final DotGraphQLContext context) throws Exception {
        try {
            final Contentlet page = environment.getSource();
            final String pageModeAsString = (String) context.getParam("pageMode");
            final PageMode mode = PageMode.get(pageModeAsString);

            Logger.debug(this, ()-> "Fetching layout for page: " + page.getIdentifier());
            final HTMLPageAsset pageAsset = APILocator.getHTMLPageAssetAPI()
                    .fromContentlet(page);

            final Template template = APILocator.getHTMLPageAssetAPI()
                    .getTemplate(pageAsset, !mode.showLive);
            return template != null && template.isDrawed()
                    ? DotTemplateTool.themeLayout(template.getInode()) : null;
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    protected TemplateLayout onRedirect() {
        return null;
    }
}
