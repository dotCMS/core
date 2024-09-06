package com.dotcms.graphql.datafetcher;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.vavr.control.Try;
import java.util.Optional;

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

            Logger.debug(this, ()-> "Fetching file field for contentlet: " + contentlet.getIdentifier() + " field: " + var +
                    " fileAssetIdentifier: " + fileAssetIdentifier);

            Optional<Contentlet> fileAsContentOptional = APILocator.getContentletAPI()
                .findContentletByIdentifierOrFallback(fileAssetIdentifier, contentlet.isLive(), contentlet.getLanguageId(),
                    user, true);

            Contentlet fileAsset = null;

            if(fileAsContentOptional.isPresent()) {
                final Contentlet fileAsContent =
                new DotTransformerBuilder().defaultOptions().content(fileAsContentOptional.get()).build().hydrate().get(0);

                fileAsset = Try.of(()->(Contentlet)APILocator.getFileAssetAPI()
                        .fromContentlet(fileAsContent)).getOrElse(fileAsContent);
            }

            return fileAsset;
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
