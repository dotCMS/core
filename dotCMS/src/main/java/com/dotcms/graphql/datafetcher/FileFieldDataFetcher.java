package com.dotcms.graphql.datafetcher;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.portlets.contentlet.transform.ContentletToMapTransformer;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.Collections;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class FileFieldDataFetcher implements DataFetcher<Contentlet> {
    @Override
    public Contentlet get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final User user = ((DotGraphQLContext) environment.getContext()).getUser();
            final Contentlet contentlet = environment.getSource();
            final String var = environment.getField().getName();
            final String fileAssetIdentifier = (String) contentlet.get(var);

            if (!UtilMethods.isSet(fileAssetIdentifier)) {
                return null;
            }

            Contentlet fileAsContent = APILocator.getContentletAPI()
                .findContentletByIdentifier(fileAssetIdentifier, contentlet.isLive(), contentlet.getLanguageId(),
                    user, true);

            fileAsContent = new ContentletToMapTransformer(Collections.singletonList(fileAsContent)).hydrate().get(0);
            return APILocator.getFileAssetAPI().fromContentlet(fileAsContent);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
