package com.dotcms.graphql.datafetcher;

import static com.dotcms.contenttype.model.type.BaseContentType.DOTASSET;

import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.BinaryToMapTransformer;
import com.dotmarketing.util.Logger;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves one property of the binary an asset carries — {@code size}, {@code mime},
 * {@code versionPath} and the rest — directly on the asset, so a client need not descend into the
 * binary field to reach them.
 *
 * <p>The binary is named differently by base type ({@code asset} for DOTASSET content,
 * {@code fileAsset} for FILEASSET), which is the reason these properties could not simply be
 * declared once and read by name.
 *
 * <p><b>The derivation is cached per contentlet, per request.</b> {@link BinaryToMapTransformer}'s
 * constructor runs the full transformer pipeline, so deriving it once per property selected would
 * make the cost of reading an asset grow with the size of the selection — ten properties meaning
 * ten full derivations, per asset, per row of a result set. That is the cost issue #34540 rules
 * out, and the reason the earlier attempt at this (PR #35363) was not carried over as written.
 *
 * <p>The cache lives on the request's {@link DotGraphQLContext}, so it cannot leak between
 * requests or between users.
 */
public class AssetBinaryPropertyDataFetcher implements DataFetcher<Object> {

    private static final String CACHE_PARAM = "assetBinaryMaps";

    @Override
    public Object get(final DataFetchingEnvironment environment) {
        final Contentlet contentlet = environment.getSource();
        if (null == contentlet) {
            return null;
        }

        try {
            final Map<String, Object> binaryMap = binaryMapOf(contentlet, environment);
            return null == binaryMap ? null : binaryMap.get(environment.getField().getName());
        } catch (final IllegalArgumentException e) {
            Logger.warn(this, "No binary on contentlet " + contentlet.getIdentifier()
                    + " for field: " + environment.getField().getName());
            return null;
        } catch (final Exception e) {
            Logger.error(this, e.getMessage(), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> binaryMapOf(final Contentlet contentlet,
            final DataFetchingEnvironment environment) {

        final Map<String, Map<String, Object>> cache = cacheFor(environment);
        if (null == cache) {
            return derive(contentlet);
        }

        // computeIfAbsent rather than get/put: field resolution may run on several threads within
        // one execution, and deriving the same binary twice is exactly what this cache exists to
        // prevent.
        return cache.computeIfAbsent(contentlet.getInode(), inode -> {
            final Map<String, Object> derived = derive(contentlet);
            return null == derived ? Collections.emptyMap() : derived;
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> derive(final Contentlet contentlet) {
        final String binaryVar = DOTASSET == contentlet.getContentType().baseType()
                ? "asset"
                : FileAssetContentType.FILEASSET_FILEASSET_FIELD_VAR;

        Logger.debug(this, () -> "Deriving binary properties for contentlet: "
                + contentlet.getIdentifier());

        return (Map<String, Object>) new BinaryToMapTransformer(contentlet).asMap()
                .get(binaryVar + "Map");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> cacheFor(final DataFetchingEnvironment environment) {
        if (!(environment.getContext() instanceof DotGraphQLContext)) {
            return null;
        }
        final DotGraphQLContext context = environment.getContext();
        synchronized (context) {
            Object cache = context.getParam(CACHE_PARAM);
            if (null == cache) {
                cache = new ConcurrentHashMap<String, Map<String, Object>>();
                context.addParam(CACHE_PARAM, cache);
            }
            return (Map<String, Map<String, Object>>) cache;
        }
    }
}
