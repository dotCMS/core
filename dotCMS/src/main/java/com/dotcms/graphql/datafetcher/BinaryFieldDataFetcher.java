package com.dotcms.graphql.datafetcher;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.BinaryToMapTransformer;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class BinaryFieldDataFetcher implements DataFetcher<Map<String, Object>> {
    @Override
    public Map<String, Object> get(DataFetchingEnvironment environment) throws Exception {
        final Contentlet contentlet = environment.getSource();
        final String var = environment.getField().getName();
        final BinaryToMapTransformer transformer = new BinaryToMapTransformer(contentlet);
        return (Map<String, Object>) ((Map)transformer.asMap().get(var)).get(var+"Map");
    }
}
