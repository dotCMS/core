package com.dotcms.graphql.datafetcher;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.FolderToMapTransformer;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class FolderFieldDataFetcher implements DataFetcher<Map<String, Object>> {
    @Override
    public Map<String, Object> get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final User user = ((DotGraphQLContext) environment.getContext()).getUser();
            final Contentlet contentlet = environment.getSource();
            Logger.debug(this, ()-> "Fetching folder for contentlet: " + contentlet.getIdentifier());
            final Map<String, Object> folderMap = (Map<String, Object>) new FolderToMapTransformer(contentlet, user)
                .asMap().get("folderMap");

            return folderMap;
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
