package com.dotcms.graphql.datafetcher;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.util.HashMap;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class SiteFieldDataFetcher implements DataFetcher<Contentlet> {
    @Override
    public Contentlet get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final User user = ((DotGraphQLContext) environment.getContext()).getUser();
            final Contentlet contentlet = environment.getSource();

            Logger.debug(this, ()-> "Fetching site for contentlet: " + contentlet.getIdentifier());
            final Host host = APILocator.getHostAPI().find(contentlet.getHost(), user, true);

            final DotContentletTransformer transformer = new DotTransformerBuilder()
                    .graphQLDataFetchOptions().content(host).build();

            final Contentlet hydratedHost = transformer.hydrate().get(0);

            final Map<String, Object> innerMap = hydratedHost.getMap();
            innerMap.put("hostId", host.getIdentifier());
            innerMap.put("hostName", host.getHostname());
            innerMap.put("hostAliases", host.getAliases());
            innerMap.put("hostTagStorage", host.getTagStorage());

            return hydratedHost;
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
