package com.dotcms.graphql.datafetcher;

import com.dotcms.graphql.CategoryToMapTransformer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.util.List;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class CategoryFieldDataFetcher implements DataFetcher<List<Map<String, Object>>> {
    @Override
    public List<Map<String, Object>> get(DataFetchingEnvironment environment) throws Exception {
        // TODO: Remove duplication with https://github.com/dotCMS/core/blob/poc-transformers-more-than-meets-the-eye
        final Contentlet contentlet = environment.getSource();
        final String var = environment.getField().getName();

        final CategoryToMapTransformer transformer = new CategoryToMapTransformer(contentlet, APILocator.systemUser());
        return (List<Map<String, Object>>) ((Map)transformer.asMap().get(var)).get("categories");

    }
}
