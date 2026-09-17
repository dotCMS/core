package com.dotcms.graphql.datafetcher;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

/**
 * Hands back the asset {@link Contentlet} that an Image or File field points at, so it can be
 * described by its real content type rather than by the flat {@code DotFileasset} view.
 *
 * <p>There is deliberately no lookup here. {@link FileFieldDataFetcher} has already resolved and
 * hydrated the referenced contentlet and passes it down as the source, so this only unwraps it.
 * Resolving it a second time would make the cost of reading an asset grow with the number of
 * properties selected, which issue #34540 explicitly rules out.
 *
 * @see com.dotcms.graphql.InterfaceType#ASSET_CONTENT_INTERFACE_NAME
 */
public class AssetContentDataFetcher implements DataFetcher<Contentlet> {

    @Override
    public Contentlet get(final DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
