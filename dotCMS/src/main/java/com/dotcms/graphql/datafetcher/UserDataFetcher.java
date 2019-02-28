package com.dotcms.graphql.datafetcher;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class UserDataFetcher implements DataFetcher<Map<String, String>> {
    @Override
    public Map<String, String> get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final Contentlet contentlet = environment.getSource();
            final String var = environment.getField().getName();
            return (Map<String, String>) contentlet.get(var+"Object");
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}