package com.dotcms.analytics.attributes;

import com.dotcms.analytics.metrics.EventType;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import org.jetbrains.annotations.NotNull;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
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
@ApplicationScoped
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
    @Inject
    public CustomAttributeAPIImpl(final CustomAttributeFactory customAttributeFactory,
                                  final CustomAttributeCache customAttributeCache) {
        this.customAttributeFactory = customAttributeFactory;
        this.customAttributeCache = customAttributeCache;

        Logger.debug(CustomAttributeAPIImpl.class, () -> "Initializing CustomAttributeAPIImpl and loading cache");
        loadCache();
        Logger.info(CustomAttributeAPIImpl.class, () -> "CustomAttributeAPIImpl initialized");
    }

    /**
     * Loads all mappings into the cache from the underlying storage.
     * Wraps any {@link DotDataException} in a {@link DotRuntimeException}.
     */
    @CloseDBIfOpened
    private void loadCache() {
        try {
            Logger.debug(CustomAttributeAPIImpl.class, () -> "Loading custom attribute mappings into cache");
            final Map<String, Map<String, String>> all = customAttributeFactory.getAll();
            all.forEach((key, value) -> customAttributeCache.put(key, value));
            Logger.info(CustomAttributeAPIImpl.class, () -> "Loaded " + all.size() + " event type mapping(s) into cache");
        } catch (DotDataException e) {
            Logger.error(CustomAttributeAPIImpl.class, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @WrapInTransaction
    public synchronized void checkCustomPayloadValidation(final String eventTypeName,
                                             final Map<String, Object> customPayload)
            throws MaxCustomAttributesReachedException, DotDataException {

        Logger.debug(CustomAttributeAPIImpl.class, () ->
                "Validating custom payload for eventType='" + eventTypeName +"' with " + (customPayload != null ? customPayload.size() : 0) + " attribute(s)");
        Map<String, String> currentCustomAttributesMatch = getCustomAttributesMatchFromCache(eventTypeName);

        final List<String> newCustomAttributes = getNewlyAttributes(customPayload, currentCustomAttributesMatch);

        Logger.debug(CustomAttributeAPIImpl.class, () ->
                "Discovered " + newCustomAttributes.size() + " new attribute(s) for eventType='" + eventTypeName + "': " + newCustomAttributes);

        Map<String, String> customAttributesMatch = currentCustomAttributesMatch != null ?
                new HashMap<>(currentCustomAttributesMatch) : new HashMap<>();

        if (!newCustomAttributes.isEmpty()) {
            assignMatchToNewAttributes(eventTypeName, customAttributesMatch, newCustomAttributes);
        }

        int currentTotalMatch = customAttributesMatch.size();
        Logger.debug(CustomAttributeAPIImpl.class, () ->
                "Current total attribute mappings for eventType='" + eventTypeName + "' is " + currentTotalMatch);

        if (currentTotalMatch > MAX_LIMIT_CUSTOM_ATTRIBUTE_BY_EVENT) {
            Logger.warn(CustomAttributeAPIImpl.class,
                    () -> "Max custom attributes reached for eventType='" + eventTypeName + "': " + currentTotalMatch + "/" + MAX_LIMIT_CUSTOM_ATTRIBUTE_BY_EVENT);
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
            Logger.debug(CustomAttributeAPIImpl.class, () -> "Cache miss for eventType='" + eventTypeName + "'. Reloading cache...");
            loadCache();
            customAttributesMatchFromCache = customAttributeCache.get(eventTypeName);

            final int size = customAttributesMatchFromCache == null ? 0 : customAttributesMatchFromCache.size();
            Logger.debug(CustomAttributeAPIImpl.class, () -> "Cache populated for eventType='" + eventTypeName + "' after reload. Mappings: " + size);
        } else {
            Logger.debug(CustomAttributeAPIImpl.class, "Cache hit for eventType='" + eventTypeName + "' with " + customAttributesMatchFromCache.size() + " mapping(s)");
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
        Logger.debug(CustomAttributeAPIImpl.class, () ->
                "Assigning matches starting at index=" + (customAttributesMatch.size() + 1) + " for new attribute(s) of eventType='" + eventTypeName + "': " + newCustomAttributes);

        for (final String newCustomAttribute : newCustomAttributes) {
            customAttributesMatch.put(newCustomAttribute, CUSTOM_ATTRIBUTE_KEY + index);
            index++;
        }

        customAttributeFactory.save(eventTypeName, customAttributesMatch);
        Logger.info(CustomAttributeAPIImpl.class, () -> "Saved attribute mapping(s) for eventType='" + eventTypeName + "'. Total mappings now: " + customAttributesMatch.size());
        customAttributeCache.clearCache();
        Logger.debug(CustomAttributeAPIImpl.class, () -> "Custom attribute cache cleared after saving mappings for eventType='" + eventTypeName + "'");
    }

    /**
     * Computes the list of attribute names present in the payload but absent from the cached mapping.
     */
    private static List<String> getNewlyAttributes(
            final Map<String, Object> customPayload,
            final Map<String, String> customAttributesMatch) {

        final Set<String> oldCustomEvents = customAttributesMatch != null ?
                customAttributesMatch.keySet() : new HashSet<>();

        final List<String> newCustomAttributes = customPayload.keySet().stream()
                .filter(key -> !oldCustomEvents.contains(key))
                .collect(Collectors.toList());
        return newCustomAttributes;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Object> translateToDatabase(final String eventTypeName,
                                                   final Map<String, Object> customPayload) {
        Logger.debug(CustomAttributeAPIImpl.class, () -> "Translating custom payload to DB columns for eventType='" + eventTypeName + "' with " + (customPayload != null ? customPayload.size() : 0) + " attribute(s)");
        final Map<String, Object> translateCustomPayload = new HashMap<>();

        final Map<String, String> currentCustomAttributesMatch = getCustomAttributesMatchFromCache(eventTypeName);

        if (currentCustomAttributesMatch == null) {
            Logger.warn(CustomAttributeAPIImpl.class, () -> "Missing attribute mapping for eventType='" + eventTypeName + "'");
            throw new MissingCustomAttributeMatchException(eventTypeName);
        }

        for (String customAttributeName : customPayload.keySet()) {
            final String databaseMatch = currentCustomAttributesMatch.get(customAttributeName);

            if (databaseMatch == null) {
                Logger.warn(CustomAttributeAPIImpl.class, () -> "Invalid attribute '" + customAttributeName + "' for eventType='" + eventTypeName + "'");
                throw new InvalidAttributeException(eventTypeName, customAttributeName);
            }

            translateCustomPayload.put(databaseMatch, customPayload.get(customAttributeName));
        }

        Logger.debug(CustomAttributeAPIImpl.class, () -> "Translated " + translateCustomPayload.size() + " attribute(s) for eventType='" + eventTypeName + "'");
        return translateCustomPayload;
    }
}
