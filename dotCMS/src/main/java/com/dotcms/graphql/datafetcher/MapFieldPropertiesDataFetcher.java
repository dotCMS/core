package com.dotcms.graphql.datafetcher;

import com.dotmarketing.util.Logger;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class MapFieldPropertiesDataFetcher implements DataFetcher<Object> {
    @Override
    public Object get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final Map<String, Object> map = environment.getSource();
            final String var = environment.getField().getName();
            Logger.debug(this, ()-> "Fetching map field for contentlet: " + map.get("identifier") + " field: " + var);
            return map.get(var);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
