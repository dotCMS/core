package com.dotcms.graphql.datafetcher.page;

import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.util.Logger;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class PageDataFetcher implements DataFetcher<Object> {
    @Override
    public Object get(final DataFetchingEnvironment environment) throws Exception {
        try {

            Logger.info(this, environment.toString());


            return CollectionsUtils.map("archived", "true");
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
