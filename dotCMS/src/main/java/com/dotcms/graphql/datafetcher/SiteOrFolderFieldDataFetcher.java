package com.dotcms.graphql.datafetcher;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.FolderToMapTransformer;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.liferay.portal.model.User;

import java.util.HashMap;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class SiteOrFolderFieldDataFetcher implements DataFetcher<Map<String, Object>> {
    @Override
    public Map<String, Object> get(final DataFetchingEnvironment environment) throws Exception {
        final User user = ((DotGraphQLContext) environment.getContext()).getUser();
        final Contentlet contentlet = environment.getSource();
        final Map<String, Object> siteOrFolderMap;

        if(!contentlet.getFolder().equals(FolderAPI.SYSTEM_FOLDER)) {
            siteOrFolderMap = (Map<String, Object>) new FolderToMapTransformer(contentlet, user)
                .asMap().get("folderMap");
        } else {
            final Host host = APILocator.getHostAPI().find(contentlet.getHost(), user, true);
            siteOrFolderMap = new HashMap<>();
            siteOrFolderMap.put("id", host.getIdentifier());
            siteOrFolderMap.put("name", host.getHostname());
            siteOrFolderMap.put(Host.ALIASES_KEY, host.getAliases());
            siteOrFolderMap.put(Host.TAG_STORAGE, host.getTagStorage());
        }

        return siteOrFolderMap;
    }
}
