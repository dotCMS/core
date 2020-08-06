package com.dotcms.graphql.datafetcher;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.FolderToMapTransformer;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class QueryMetadataDataFetcher implements DataFetcher<List<Map<String, Object>>> {
    @Override
    public List<Map<String, Object>> get(final DataFetchingEnvironment environment) throws Exception {
        try {
            return ((DotGraphQLContext) environment.getContext()).getFieldCountMaps();
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
