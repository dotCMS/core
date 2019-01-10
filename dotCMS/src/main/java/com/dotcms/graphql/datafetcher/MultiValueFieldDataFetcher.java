package com.dotcms.graphql.datafetcher;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.util.Arrays;
import java.util.List;

public class MultiValueFieldDataFetcher implements DataFetcher<List<String>> {
    @Override
    public List<String> get(DataFetchingEnvironment environment) throws Exception {
        final Contentlet contentlet = environment.getSource();
        final String var = environment.getField().getName();
        final String values = (String) contentlet.get(var);
        return Arrays.asList(values.split("\\s*,\\s*"));
    }
}
