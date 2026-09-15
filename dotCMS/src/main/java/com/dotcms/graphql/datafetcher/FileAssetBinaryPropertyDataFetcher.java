package com.dotcms.graphql.datafetcher;

import static com.dotcms.contenttype.model.type.BaseContentType.DOTASSET;

import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.BinaryToMapTransformer;
import com.dotmarketing.util.Logger;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;

/**
 * Resolves a single {@code DotBinaryLike} interface field (e.g. {@code versionPath},
 * {@code size}, {@code mime}) against a {@link Contentlet} that represents a FileAsset or a
 * DotAsset.
 *
 * <p>The backing data is produced by {@link BinaryToMapTransformer}, which is the same
 * transformer used by {@code BinaryFieldDataFetcher} for raw {@code BinaryField}s. That guarantees
 * the values returned here are identical to those a client would get by querying the equivalent
 * {@code mybinary} field on a Binary-typed content property — which is the whole point of the
 * unified interface.
 *
 * <p>The binary map key depends on the underlying contentlet's base type: {@code "assetMap"} for
 * {@code DotAsset} and {@code "fileAssetMap"} for {@code FileAsset}.
 */
public class FileAssetBinaryPropertyDataFetcher implements DataFetcher<Object> {

    @Override
    public Object get(final DataFetchingEnvironment environment) throws Exception {
        final Contentlet contentlet = environment.getSource();
        if (contentlet == null) {
            return null;
        }
        try {
            final String fieldName = environment.getField().getName();
            final Map<String, Object> binaryMap = resolveBinaryMap(contentlet);
            return binaryMap != null ? binaryMap.get(fieldName) : null;
        } catch (IllegalArgumentException e) {
            Logger.warn(this, "Binary is null for field: " + environment.getField().getName());
            return null;
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveBinaryMap(final Contentlet contentlet) {
        final String var = contentlet.getContentType().baseType() == DOTASSET
                ? "asset"
                : FileAssetContentType.FILEASSET_FILEASSET_FIELD_VAR;
        final BinaryToMapTransformer transformer = new BinaryToMapTransformer(contentlet);
        return (Map<String, Object>) transformer.asMap().get(var + "Map");
    }
}
