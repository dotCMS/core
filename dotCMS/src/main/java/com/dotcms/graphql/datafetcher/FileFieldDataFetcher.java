package com.dotcms.graphql.datafetcher;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.graphql.InterfaceType;
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
        return resolve(environment, environment.getField().getName());
    }

    /**
     * Resolves the contentlet referenced by {@code var} on the environment's source.
     *
     * <p>Split out from {@link #get(DataFetchingEnvironment)} so a companion field can reuse it:
     * such a field sits beside the asset field rather than inside it, so its own name is not the
     * name of the field holding the identifier and it must pass that variable explicitly.
     */
    public Contentlet resolve(final DataFetchingEnvironment environment, final String var)
            throws Exception {
        try {
            final User user = ((DotGraphQLContext) environment.getContext()).getUser();
            final Contentlet contentlet = environment.getSource();
            final String fileAssetIdentifier = (String) contentlet.get(var);

            if (!UtilMethods.isSet(fileAssetIdentifier)) {
                return null;
            }

            Logger.debug(this, ()-> "Fetching file field for contentlet: " + contentlet.getIdentifier() + " field: " + var +
                    " fileAssetIdentifier: " + fileAssetIdentifier);

            Optional<Contentlet> fileAsContentOptional = APILocator.getContentletAPI()
                .findContentletByIdentifierOrFallback(fileAssetIdentifier, contentlet.isLive(), contentlet.getLanguageId(),
                    user, true);

            Contentlet resolved = null;

            if(fileAsContentOptional.isPresent()) {
                final Contentlet fileAsContent =
                new DotTransformerBuilder().defaultOptions().content(fileAsContentOptional.get()).build().hydrate().get(0);

                resolved = Try.of(()->(Contentlet)APILocator.getFileAssetAPI()
                        .fromContentlet(fileAsContent)).getOrElse(fileAsContent);
            }

            final Contentlet fileAsset = resolved;
            if (null != fileAsset
                    && !InterfaceType.isAssetBaseType(fileAsset.getContentType().baseType())) {
                // The field holds the identifier of ordinary content. Nothing prevents that: the
                // value is a bare identifier and the conversion above falls back to the raw
                // contentlet. Since this field is described by the asset interface, handing such a
                // contentlet on would make the type resolver name an object type outside the
                // interface, and graphql-java answers that by failing the WHOLE request -- one
                // mis-pointed field taking the rest of the query down with it. The
                // misconfiguration is in the data, so the field reports nothing instead.
                Logger.debug(FileFieldDataFetcher.class, () -> "Field '" + var
                        + "' points at content that is not an asset: "
                        + fileAsset.getContentType().variable()
                        + ". Reporting nothing rather than failing the request.");
                return null;
            }

            return fileAsset;
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }
}
