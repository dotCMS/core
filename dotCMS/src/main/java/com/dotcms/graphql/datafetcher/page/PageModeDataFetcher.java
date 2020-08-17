package com.dotcms.graphql.datafetcher.page;

import com.dotmarketing.portlets.htmlpageasset.business.render.page.ViewAsPageStatus;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;

/**
 * This DataFetcher returns the {@link PageMode} of the requested page as a String.
 */
public class PageModeDataFetcher implements DataFetcher<String> {
    @Override
    public String get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final ViewAsPageStatus viewAsPageStatus = environment.getSource();
            return viewAsPageStatus.getPageMode().name();
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
