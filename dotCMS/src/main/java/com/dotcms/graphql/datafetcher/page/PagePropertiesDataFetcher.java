package com.dotcms.graphql.datafetcher.page;

import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.util.Logger;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class PagePropertiesDataFetcher implements DataFetcher<Object> {
    @Override
    public Object get(final DataFetchingEnvironment environment) throws Exception {
        try {

            environment.getSource();

            return CollectionsUtils.map("archived", "true");
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
