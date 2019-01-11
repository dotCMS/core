package com.dotcms.graphql.datafetcher;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class FieldDataFetcher implements DataFetcher<Object> {
    @Override
    public Object get(final DataFetchingEnvironment environment) throws Exception {
        final Contentlet contentlet = environment.getSource();
        final String var = environment.getField().getName();
        return contentlet.get(var);
    }
}
