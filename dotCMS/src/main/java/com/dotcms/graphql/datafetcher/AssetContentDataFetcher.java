package com.dotcms.graphql.datafetcher;

import com.dotcms.graphql.InterfaceType;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
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
 * <p>Content that is not an asset at all resolves to {@code null}. Nothing prevents an Image or
 * File field from holding the identifier of ordinary content — the field stores a bare identifier
 * and {@link FileFieldDataFetcher} falls back to the raw contentlet when
 * {@code FileAssetAPI.fromContentlet} cannot convert it. Handing such a contentlet on would make
 * the type resolver name an object type that does not implement this interface, and graphql-java
 * answers that with an {@code UnresolvedTypeException} that fails the <b>whole request</b> — one
 * mis-pointed field taking the rest of the query down with it. The misconfiguration is in the
 * data, so the field reports nothing and the rest of the response is delivered.
 *
 * @see com.dotcms.graphql.InterfaceType#ASSET_CONTENT_INTERFACE_NAME
 */
public class AssetContentDataFetcher implements DataFetcher<Contentlet> {

    @Override
    public Contentlet get(final DataFetchingEnvironment environment) {
        final Contentlet contentlet = environment.getSource();

        if (null == contentlet || !InterfaceType.isAssetBaseType(contentlet.getContentType().baseType())) {
            Logger.debug(this, () -> "Asset field points at content that is not an asset: "
                    + (null == contentlet ? "null" : contentlet.getContentType().variable())
                    + ". Reporting no asset content rather than failing the request.");
            return null;
        }

        return contentlet;
    }
}
