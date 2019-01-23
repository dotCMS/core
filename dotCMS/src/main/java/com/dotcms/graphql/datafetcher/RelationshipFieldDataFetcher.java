package com.dotcms.graphql.datafetcher;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.List;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class RelationshipFieldDataFetcher implements DataFetcher<List<? extends Contentlet>> {
    @Override
    public List<? extends Contentlet> get(final DataFetchingEnvironment environment) throws Exception {
        final Contentlet contentlet = environment.getSource();
        final String fieldVar = environment.getField().getName();
        return contentlet.getRelated(fieldVar);
    }
}
