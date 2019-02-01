package com.dotcms.graphql.datafetcher;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

import java.util.HashMap;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class SiteFieldDataFetcher implements DataFetcher<Map<String, Object>> {
    @Override
    public Map<String, Object> get(final DataFetchingEnvironment environment) throws Exception {
        final User user = ((DotGraphQLContext) environment.getContext()).getUser();
        final Contentlet contentlet = environment.getSource();
        final Map<String, Object> siteMap = new HashMap<>();

        final Host host = APILocator.getHostAPI().find(contentlet.getHost(), user, true);

        siteMap.put("hostId", host.getIdentifier());
        siteMap.put("hostName", host.getHostname());
        siteMap.put("hostAliases", host.getAliases());
        siteMap.put("hostTagStorage", host.getTagStorage());

        return siteMap;
    }
}
