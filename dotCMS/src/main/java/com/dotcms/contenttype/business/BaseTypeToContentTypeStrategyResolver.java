package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;

/**
 * Subscribe Strategies to resolve from base types a content type
 * and get the strategy for a set of arguments if applies
 * @author jsanca
 */
public class BaseTypeToContentTypeStrategyResolver {

    private volatile Map<BaseContentType, BaseTypeToContentTypeStrategy> strategiesMap = this.getDefaultStrategies();

    private  Map<BaseContentType, BaseTypeToContentTypeStrategy> getDefaultStrategies() {

        final ImmutableMap.Builder<BaseContentType, BaseTypeToContentTypeStrategy> builder =
                new ImmutableMap.Builder<>();

        builder.put(BaseContentType.DOTASSET,  new DotAssetBaseTypeToContentTypeStrategyImpl());
        builder.put(BaseContentType.FILEASSET, new FileAssetBaseTypeToContentTypeStrategyImpl());

        return builder.build();
    }


    public synchronized void subscribe (final BaseContentType baseContentType, final BaseTypeToContentTypeStrategy strategy) {

        if (null != baseContentType && null != strategy) {

            final ImmutableMap.Builder<BaseContentType, BaseTypeToContentTypeStrategy> builder =
                    new ImmutableMap.Builder<>();

            // keep every existing mapping except the one being (re)subscribed, then register the new strategy
            this.strategiesMap.forEach((key, value) -> {
                if (!key.equals(baseContentType)) {
                    builder.put(key, value);
                }
            });
            builder.put(baseContentType, strategy);

            this.strategiesMap = builder.build();
        }
    }


    private static class SingletonHolder {
        private static final BaseTypeToContentTypeStrategyResolver INSTANCE = new BaseTypeToContentTypeStrategyResolver();
    }

    /**
     * Get the instance.
     * @return BaseTypeToContentTypeStrategyResolver
     */
    public static BaseTypeToContentTypeStrategyResolver getInstance() {

        return BaseTypeToContentTypeStrategyResolver.SingletonHolder.INSTANCE;
    } // getInstance.


    /**
     * Get a strategy if applies
     * @param baseContentType {@link BaseContentType}
     * @return Optional BaseTypeToContentTypeStrategy
     */
    public Optional<BaseTypeToContentTypeStrategy> get(final BaseContentType baseContentType) {

        return this.strategiesMap.containsKey(baseContentType)?
                Optional.of(this.strategiesMap.get(baseContentType)):
                Optional.empty();
    }

}
