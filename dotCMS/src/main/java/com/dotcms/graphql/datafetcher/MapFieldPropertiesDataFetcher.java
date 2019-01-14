package com.dotcms.graphql.datafetcher;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class MapFieldPropertiesDataFetcher implements DataFetcher<Object> {
    @Override
    public Object get(final DataFetchingEnvironment environment) throws Exception {
        final Map<String, Object> map = environment.getSource();
        final String var = environment.getField().getName();
        return map.get(var);
    }
}
