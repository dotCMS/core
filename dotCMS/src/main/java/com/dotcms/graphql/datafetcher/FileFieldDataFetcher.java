package com.dotcms.graphql.datafetcher;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class FileFieldDataFetcher implements DataFetcher<Contentlet> {
    @Override
    public Contentlet get(final DataFetchingEnvironment environment) throws Exception {
        final User user = ((DotGraphQLContext) environment.getContext()).getUser();
        final Contentlet contentlet = environment.getSource();
        final String var = environment.getField().getName();
        final String fileAssetIdentifier = (String) contentlet.get(var);
        final Contentlet fileAsContent = APILocator.getContentletAPI()
            .findContentletByIdentifier(fileAssetIdentifier, contentlet.isLive(), contentlet.getLanguageId(),
                user, true);
        return  APILocator.getFileAssetAPI().fromContentlet(fileAsContent);
    }
}
