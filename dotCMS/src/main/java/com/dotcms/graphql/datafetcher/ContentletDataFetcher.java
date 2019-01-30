package com.dotcms.graphql.datafetcher;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.ContentletToMapTransformer;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.List;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class ContentletDataFetcher implements DataFetcher<List<Contentlet>> {
    @Override
    public List<Contentlet> get(final DataFetchingEnvironment environment) throws Exception {
        final User user = ((DotGraphQLContext) environment.getContext()).getUser();
        final String query = environment.getArgument("query");
        final Integer limit = environment.getArgument("limit")!=null ? environment.getArgument("limit") : 0;
        final Integer offset = environment.getArgument("offset")!=null ? environment.getArgument("offset") : -1;
        final String sortBy = environment.getArgument("sortBy");
        final List<Contentlet> contentletList = APILocator.getContentletAPI().search(query, limit, offset, sortBy,
            user, true);
        return new ContentletToMapTransformer(contentletList).hydrate();
    }
}
