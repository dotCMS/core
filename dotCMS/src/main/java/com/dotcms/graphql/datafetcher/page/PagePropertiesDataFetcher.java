package com.dotcms.graphql.datafetcher.page;

import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl.HTMLPageUrl;
import com.dotmarketing.util.Logger;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class PagePropertiesDataFetcher implements DataFetcher<Object> {
    @Override
    public Object get(final DataFetchingEnvironment environment) throws Exception {
        try {
            HTMLPageUrl pageUrl = environment.getSource();
            final String field = environment.getField().getName();
            return pageUrl.getHTMLPage().getMap().get(field);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
