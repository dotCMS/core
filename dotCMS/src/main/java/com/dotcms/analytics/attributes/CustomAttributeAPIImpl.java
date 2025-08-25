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

/**
 * Default implementation of {@link CustomAttributeAPI} backed by database persistence and a cache.
 * <p>
 * Responsibilities include:
 * <ul>
 *   <li>Loading/storing attribute mappings between attribute names and database columns per event type.</li>
 *   <li>Validating incoming custom attribute payloads and enforcing the maximum attributes constraint.</li>
 *   <li>Translating payloads to database column keys.</li>
 * </ul>
 */
public class CustomAttributeAPIImpl implements CustomAttributeAPI {
    /** Prefix used to build database column names for custom attributes. */
    public static String CUSTOM_ATTRIBUTE_KEY = "custom_";
    /** Maximum allowed custom attributes per event type. */
    private final int MAX_LIMIT_CUSTOM_ATTRIBUTE_BY_EVENT = 50;
    private final CustomAttributeCache customAttributeCache;
    private final CustomAttributeFactory customAttributeFactory;

    /**
     * Creates an instance using the default factory and cache providers.
     */
    public CustomAttributeAPIImpl() {
        this(FactoryLocator.getAnalyticsCustomAttributeFactory(), CacheLocator.getAnalyticsCustomAttributeCache());
    }

    /**
     * Creates an instance with the provided dependencies.
     *
     * @param customAttributeFactory the persistence access for attribute mappings.
     * @param customAttributeCache the cache used to speed up mapping lookup.
     */
    public CustomAttributeAPIImpl(final CustomAttributeFactory customAttributeFactory,
                                  final CustomAttributeCache customAttributeCache) {
        this.customAttributeFactory = customAttributeFactory;
        this.customAttributeCache = customAttributeCache;

        loadCache();
    }

    /**
     * Loads all mappings into the cache from the underlying storage.
     * Wraps any {@link DotDataException} in a {@link DotRuntimeException}.
     */
    private void loadCache() {
        try {
            customAttributeFactory.getAll()
                    .forEach((key, value) -> customAttributeCache.put(key, value));
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    /** {@inheritDoc} */
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

    /**
     * Retrieves the attribute mapping for the provided event type from the cache, attempting to
     * lazily load the cache if a miss occurs.
     *
     * @param eventTypeName the event type name.
     * @return the mapping of attribute name to database column, or {@code null} if not found.
     */
    private Map<String, String> getCustomAttributesMatchFromCache(String eventTypeName) {
        Map<String, String> customAttributesMatchFromCache = customAttributeCache.get(eventTypeName);

        if (customAttributesMatchFromCache == null) {
            loadCache();
            customAttributesMatchFromCache = customAttributeCache.get(eventTypeName);
        }
        return customAttributesMatchFromCache;
    }

    /**
     * Persists and caches new attribute mappings for the given event type.
     */
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

    /**
     * Computes the list of attribute names present in the payload but absent from the cached mapping.
     */
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

    /** {@inheritDoc} */
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
