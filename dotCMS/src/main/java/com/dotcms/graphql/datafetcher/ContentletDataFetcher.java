package com.dotcms.graphql.datafetcher;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.ContentletToMapTransformer;

import java.util.List;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class ContentletDataFetcher implements DataFetcher<List<Contentlet>> {
    @Override
    public List<Contentlet> get(final DataFetchingEnvironment environment) throws Exception {
        final String query = environment.getArgument("query");
        final List<Contentlet> contentletList = APILocator.getContentletAPI().search(query, 0, -1, null,
            APILocator.systemUser(), false);
        return new ContentletToMapTransformer(contentletList).hydrate();
    }
}
