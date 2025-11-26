package com.dotcms.graphql.datafetcher;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.util.Logger;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;

/**
 * This DataFetcher provides the pagination for each requested field in a GraphQL Query for the
 * Content Delivery API
 */
public class PaginationsDataFetcher implements DataFetcher<List<Map<String, Object>>> {

    @Override
    public List<Map<String, Object>> get(final DataFetchingEnvironment environment)
            throws Exception {

        try {
            return ((DotGraphQLContext) environment.getContext()).getFieldPaginationMaps();
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }

}
