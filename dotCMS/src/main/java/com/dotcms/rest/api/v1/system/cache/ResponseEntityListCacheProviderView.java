package com.dotcms.rest.api.v1.system.cache;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.MessageEntity;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.Pagination;
import com.dotmarketing.business.cache.provider.CacheProvider;
import java.util.List;
import java.util.Map;

/**
 * ResponseEntityView for List<CacheProvider> responses.
 * Provides proper type information for OpenAPI/Swagger documentation.
 */
public class ResponseEntityListCacheProviderView extends ResponseEntityView<List<CacheProvider>> {

    public ResponseEntityListCacheProviderView(List<CacheProvider> entity) {
        super(entity);
    }

    public ResponseEntityListCacheProviderView(List<CacheProvider> entity, Pagination pagination) {
        super(entity, pagination);
    }

    public ResponseEntityListCacheProviderView(List<CacheProvider> entity, String... permissions) {
        super(entity, permissions);
    }

    public ResponseEntityListCacheProviderView(List<CacheProvider> entity, Map<String, String> i18nMessagesMap) {
        super(entity, i18nMessagesMap);
    }

    public ResponseEntityListCacheProviderView(List<CacheProvider> entity, List<MessageEntity> messages) {
        super(entity, messages);
    }

    public ResponseEntityListCacheProviderView(List<CacheProvider> entity, List<MessageEntity> messages, Map<String, String> i18nMessagesMap) {
        super(entity, messages, i18nMessagesMap);
    }

    public ResponseEntityListCacheProviderView(List<CacheProvider> entity, List<ErrorEntity> errors, List<MessageEntity> messages) {
        super(entity, errors, messages);
    }

    public ResponseEntityListCacheProviderView(List<CacheProvider> entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap) {
        super(entity, errors, messages, i18nMessagesMap);
    }

    public ResponseEntityListCacheProviderView(List<CacheProvider> entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap, List<String> permissions) {
        super(entity, errors, messages, i18nMessagesMap, permissions);
    }

    public ResponseEntityListCacheProviderView(List<CacheProvider> entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap, List<String> permissions, Pagination pagination) {
        super(entity, errors, messages, i18nMessagesMap, permissions, pagination);
    }
}