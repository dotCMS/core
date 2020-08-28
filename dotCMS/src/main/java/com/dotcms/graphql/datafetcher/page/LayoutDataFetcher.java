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
import com.liferay.portal.model.User;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;

/**
 * This DataFetcher returns the {@link TemplateLayout} associated to the requested {@link HTMLPageAsset}.
 */
public class LayoutDataFetcher implements DataFetcher<TemplateLayout> {
    @Override
    public TemplateLayout get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final DotGraphQLContext context = environment.getContext();
            final User user = context.getUser();
            final Contentlet page = environment.getSource();
            final String pageModeAsString = (String) context.getParam("pageMode");

            final PageMode mode = PageMode.get(pageModeAsString);

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
}
