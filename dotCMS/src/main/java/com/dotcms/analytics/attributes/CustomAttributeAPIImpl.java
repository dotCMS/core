package com.dotcms.analytics.attributes;

import com.dotcms.analytics.content.ReportResponse;
import com.dotcms.analytics.model.ResultSetItem;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    public static final String FRIENDLY_QUERY_CUSTOM_ATTRIBUTE_PREFIX = "request.custom.";
    public static final String QUERY_CUSTOM_ATTRIBUTE_PREFIX = "request.";

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
            all.forEach(customAttributeCache::put);
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

        return customPayload.keySet().stream()
                .filter(key -> !oldCustomEvents.contains(key))
                .collect(Collectors.toList());
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

    @Override
    public TranslatedQuery translateFromFriendlyName(final String query) throws CustomAttributeProcessingException {
        if (!containsCustomAttributes(query)) {
            return new TranslatedQuery(query);
        }

        final Map<String, Object> queryAsJson = getAsJson(query);

        if (!UtilMethods.isSet(queryAsJson)) {
            return new TranslatedQuery(query);
        }

        final List<String> eventsTypes = extractEventTypeFilter(queryAsJson);

        if (eventsTypes.size() != 1) {
            throw new CustomAttributeProcessingException("You must filter by one Event Type in order to resolve custom attributes");
        }

        final String eventTypeNameOptional = eventsTypes.get(0);

        final Map<String, String> customAttributesMatches = getCustomAttributesMatchFromCache(eventTypeNameOptional);
        final Map<String, String> matchApplied = new HashMap<>();

        String translateResponse = query;

        if (UtilMethods.isSet(customAttributesMatches)) {
            for (final Map.Entry<String, String> customMatch : customAttributesMatches.entrySet()) {
                final String findBy =  FRIENDLY_QUERY_CUSTOM_ATTRIBUTE_PREFIX + customMatch.getKey();
                final String replaceBy = QUERY_CUSTOM_ATTRIBUTE_PREFIX + customMatch.getValue();

                translateResponse = translateResponse.replaceAll("\"" + findBy + "\"",
                        "\"" + replaceBy + "\"");
                matchApplied.put(findBy, replaceBy);
            }
        }

        return new TranslatedQuery(translateResponse, matchApplied);
    }

    @Override
    public ReportResponse translateResults(final ReportResponse reportResponse, final Map<String, String> matchApplied){
        final List<ResultSetItem> results = reportResponse.getResults();
        final List<ResultSetItem> newResults = new ArrayList<>();

        for (final ResultSetItem result : results) {
            final Map<String, Object> map = result.getAll();
            final Map<String, Object> newMap = new HashMap<>();

            for (String key : map.keySet()) {
                final String newKey = matchApplied.getOrDefault(key, key);
                newMap.put(newKey, map.get(key));
            }

            newResults.add(new ResultSetItem(newMap));
        }

        return new ReportResponse(newResults);
    }

    /**
     * Extracts the list of event type values from a Cube.js query JSON structure.
     * <p>
     * This inspects the top-level "filters" array and recursively traverses any nested
     * logical groups ("and" / "or") collecting the values of filters whose
     * {@code member} equals {@code request.eventType}.
     *
     * @param queryAsJson the parsed Cube.js query as a map.
     * @return a list of event type strings found in the filter tree (possibly empty).
     */
    @SuppressWarnings("unchecked")
    public static List<String> extractEventTypeFilter(final Map<String, Object> queryAsJson) {

        Object filtersObject = queryAsJson.get("filters");

        if (filtersObject == null) {
            return Collections.emptyList();
        }

        final List<Map<String, Object>> filters =  List.class.isAssignableFrom(filtersObject.getClass()) ?
                (List<Map<String, Object>>) filtersObject : Collections.emptyList();

        final List<String> results = new ArrayList<>();

        for (final Map<String, Object> filter : filters) {
            collectEventTypes(filter, results);
        }

        return results;
    }

    /**
     * Parses a Cube.js query JSON string into a map structure.
     *
     * @param cubeJsQueryJson the Cube.js query as a JSON string.
     * @return the parsed map; if parsing fails, an empty map is returned.
     */
    private static Map<String, Object> getAsJson(final String cubeJsQueryJson)  {
        try {
            return JsonUtil.getJsonFromString(cubeJsQueryJson);
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    /**
     * Extracts the values for a single filter node when it targets the event type dimension.
     *
     * @param filter a single filter object from the Cube.js query.
     * @return an optional list of event type strings if the filter's member is "request.eventType";
     *         otherwise, {@link Optional#empty()}.
     */
    @SuppressWarnings("unchecked")
    private static  Optional<List<String>> getEventTypes(Map<String, Object> filter) {
        final String member = filter.get("member") != null ?
                filter.get("member").toString() : StringPool.BLANK;

        if ( member.equals("request.eventType")) {
            final List<String> values = (List<String>) filter.get("values");

            if (values != null && !values.isEmpty()) {
                return Optional.of(values);
            }
        }

        return Optional.empty();
    }

    /**
     * Recursively traverses a filter node, accumulating any event type values into the given results list.
     * <p>
     * The filter node can be a logical group ("and" / "or") or a direct condition with a "member".
     *
     * @param filterNode the current filter node (may be {@code null}).
     * @param results the mutable list to collect discovered event type values into.
     */
    private static void collectEventTypes(final Map<String, Object> filterNode, List<String> results) {
        if (filterNode == null) return;

        if (filterNode.containsKey("or")) {
            final List<Map<String, Object>> filters = getList(filterNode, "or");

            for (Map<String, Object> subFilter : filters) {
                collectEventTypes(subFilter, results);
            }
        }else if (filterNode.containsKey("and")) {
            final List<Map<String, Object>> filters = getList(filterNode, "and");

            for (Map<String, Object> subFilter : filters) {
                collectEventTypes(subFilter, results);
            }
        }
        // Case 2: Direct filter with "member" or "dimension"
        else if (filterNode.containsKey("member")) {
            getEventTypes(filterNode).ifPresent(results::addAll);

        }
    }

    /**
     * Safely retrieves a list of filter maps from the given node attribute (e.g., "and" or "or").
     *
     * @param filterNode the node containing a list attribute.
     * @param attributeName the attribute name to read (usually "and" or "or").
     * @return the list of maps if present and well-formed; otherwise an empty list.
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> getList(final Map<String, Object> filterNode, final String attributeName) {
        Object asObject = filterNode.get(attributeName);

        if (!List.class.isAssignableFrom(asObject.getClass())) {
            return Collections.emptyList();
        }

        try {
            return (List<Map<String, Object>>) asObject;
        } catch (ClassCastException e) {
            return Collections.emptyList();
        }
    }

    private static boolean containsCustomAttributes(final String cubeJsQueryJson) {
        return cubeJsQueryJson.contains(FRIENDLY_QUERY_CUSTOM_ATTRIBUTE_PREFIX);
    }

}
