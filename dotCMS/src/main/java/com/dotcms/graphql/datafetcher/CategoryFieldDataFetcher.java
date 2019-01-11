package com.dotcms.graphql.datafetcher;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.CategoryToMapTransformer;

import java.util.List;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class CategoryFieldDataFetcher implements DataFetcher<List<Map<String, Object>>> {
    @Override
    public List<Map<String, Object>> get(final DataFetchingEnvironment environment) throws Exception {
        final Contentlet contentlet = environment.getSource();
        final String var = environment.getField().getName();

        final CategoryToMapTransformer transformer = new CategoryToMapTransformer(contentlet, APILocator.systemUser());
        return (List<Map<String, Object>>) ((Map)transformer.asMap().get(var)).get("categories");

    }
}
