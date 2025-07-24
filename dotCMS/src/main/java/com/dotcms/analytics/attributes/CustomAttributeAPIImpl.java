package com.dotcms.analytics.attributes;

import com.dotcms.analytics.metrics.EventType;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class CustomAttributeAPIImpl implements CustomAttributeAPI {
    private String CUSTOM_ATTRIBUTE_KEY = "custom_";
    private final int MAX_LIMIT_CUSTOM_ATTRIBUTE_BY_EVENT = 50;
    private final CustomAttributeCache customAttributeCache;
    private final CustomAttributeFactory customAttributeFactory;

    public CustomAttributeAPIImpl() {
        this(FactoryLocator.getAnalyticsCustomAttributeFactory(), CacheLocator.getAnalyticsCustomAttributeCache());
    }

    public CustomAttributeAPIImpl(final CustomAttributeFactory customAttributeFactory,
                                  final CustomAttributeCache customAttributeCache) {
        this.customAttributeFactory = customAttributeFactory;
        this.customAttributeCache = customAttributeCache;

        loadCache();
    }

    private void loadCache() {
        try {
            customAttributeFactory.getAll()
                    .forEach((key, value) -> customAttributeCache.put(key, value));
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    @Override
    @WrapInTransaction
    @CloseDBIfOpened
    public synchronized void checkCustomPayloadValidation(final String eventTypeName,
                                             final Map<String, Object> customPayload)
            throws MaxCustomAttributesReachedException, DotDataException {

        Map<String, String> currentCustomAttributesMatch = getCustomAttributesMatchFromCache(eventTypeName);

        final List<String> newCustomAttributes = getNewlyAttributes(customPayload, currentCustomAttributesMatch);

        Map<String, String> customAttributesMatch = currentCustomAttributesMatch != null ?
                new HashMap<>(currentCustomAttributesMatch) : new HashMap<>();

        if (!newCustomAttributes.isEmpty()) {
            assignMatchToNewAttributes(eventTypeName, customAttributesMatch, newCustomAttributes);
        }

        int currentTotalMatch = customAttributesMatch.size();

        if (currentTotalMatch > MAX_LIMIT_CUSTOM_ATTRIBUTE_BY_EVENT) {
            throw new MaxCustomAttributesReachedException(eventTypeName, MAX_LIMIT_CUSTOM_ATTRIBUTE_BY_EVENT);
        }
    }

    private Map<String, String> getCustomAttributesMatchFromCache(String eventTypeName) {
        Map<String, String> customAttributesMatchFromCache = customAttributeCache.get(eventTypeName);

        if (customAttributesMatchFromCache == null) {
            loadCache();
            customAttributesMatchFromCache = customAttributeCache.get(eventTypeName);
        }
        return customAttributesMatchFromCache;
    }

    private void assignMatchToNewAttributes(
            final String eventTypeName,
            final Map<String, String> customAttributesMatch,
            final List<String> newCustomAttributes) throws DotDataException {

        int index = customAttributesMatch.size() + 1;

        for (final String newCustomAttribute : newCustomAttributes) {
            customAttributesMatch.put(newCustomAttribute, CUSTOM_ATTRIBUTE_KEY + index);
            index++;
        }

        customAttributeFactory.save(eventTypeName, customAttributesMatch);
        customAttributeCache.clearCache();
    }

    private static List<String> getNewlyAttributes(
            final Map<String, Object> customPayload,
            final Map<String, String> customAttributesMatchFromCache) {

        final Set<String> oldCustomMEvents = customAttributesMatchFromCache != null ?
                customAttributesMatchFromCache.keySet() : new HashSet<>();

        final List<String> newCustomAttributes = customPayload.keySet().stream()
                .filter(key -> !oldCustomMEvents.contains(key))
                .collect(Collectors.toList());
        return newCustomAttributes;
    }

    @Override
    public Map<String, Object> translateToDatabase(final String eventTypeName,
                                                   final Map<String, Object> customPayload) {
        final Map<String, Object> translateCustomPayload = new HashMap<>();

        final Map<String, String> currentCustomAttributesMatch = getCustomAttributesMatchFromCache(eventTypeName);

        if (currentCustomAttributesMatch == null) {
            throw new MissingCustomAttributeMatchException(eventTypeName);
        }

        for (String customAttributeName : customPayload.keySet()) {
            final String databaseMatch = currentCustomAttributesMatch.get(customAttributeName);

            if (databaseMatch == null) {
                throw new InvalidAttributeException(eventTypeName, customAttributeName);
            }

            translateCustomPayload.put(databaseMatch, customPayload.get(customAttributeName));
        }

        return translateCustomPayload;
    }
}
