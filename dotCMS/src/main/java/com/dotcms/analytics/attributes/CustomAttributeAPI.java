package com.dotcms.analytics.attributes;

import com.dotcms.analytics.content.ReportResponse;
import com.dotmarketing.exception.DotDataException;

import java.util.HashMap;

import com.dotcms.analytics.metrics.EventType;
import com.dotmarketing.exception.DotDataException;

import java.util.Map;

/**
 * API for managing and validating Analytics Custom Attributes.
 * <p>
 * Custom attributes are user-defined key/value pairs sent along with analytics events. For each
 * {@code eventTypeName}, dotCMS stores a mapping between a human-friendly attribute name
 * (e.g., "plan") and the database field that ultimately persists it (e.g., "custom_1").
 * This API is responsible for:
 * <ul>
 *   <li>Validating incoming payloads and ensuring the number of attributes per event type does not exceed the limit.</li>
 *   <li>Creating attribute mappings for new attribute keys when needed.</li>
 *   <li>Translating incoming payloads to their database column representation.</li>
 * </ul>
 */
public interface CustomAttributeAPI {

    /**
     * Validates a custom attribute payload for a specific event type. If the payload contains new
     * custom attributes that are not yet mapped, this method will create the mapping and persist it.
     * If creating the mapping would exceed the maximum number of supported attributes for the event
     * type, an exception is thrown.
     *
     * @param eventTypeName the analytics event type name to validate against.
     * @param customPayload the payload containing custom attribute key/value pairs.
     * @throws MaxCustomAttributesReachedException if the event would exceed the maximum number of custom attributes.
     * @throws DotDataException if there is a persistence error while creating/updating mappings.
     */
    void checkCustomPayloadValidation(String eventTypeName, Map<String, Object> customPayload)
            throws MaxCustomAttributesReachedException, DotDataException;

    /**
     * Translates a custom attribute payload for the given event type from human-friendly keys to
     * database column names.
     *
     * @param eventTypeName the analytics event type name to translate for.
     * @param customPayload the payload with user-defined attribute keys.
     * @return a new map with keys replaced by the database column names (e.g., custom_1, custom_2, ...).
     * @throws MissingCustomAttributeMatchException if no mapping exists for the given event type in cache/storage.
     * @throws InvalidAttributeException if one or more attributes in the payload are not known/mapped for this event type.
     */
    Map<String, Object> translateToDatabase(String eventTypeName, Map<String, Object> customPayload);

    /**
     * Translates a Cube.js query JSON string that uses human-friendly custom attribute names into a
     * query that uses underlying database column identifiers.
     * <p>
     * This method searches for occurrences of keys prefixed by {@code request.custom.} (for example,
     * {@code "request.custom.plan"}) and replaces them with their mapped column names (for example,
     * {@code "request.custom_1"}) according to the attribute mapping for the inferred event type.
     * <p>
     * If the query does not contain friendly custom attribute names or if the query cannot be parsed,
     * the original query will be returned and the map of matches will be empty.
     *
     * @param query a Cube.js query JSON string possibly containing friendly custom attribute keys.
     * @return a {@link TranslatedQuery} holding the translated query and a map of replacements
     *         that were applied (from friendly name to database column key).
     * @throws CustomAttributeProcessingException if the event type cannot be uniquely determined
     *         from the query filters and therefore the mapping for custom attributes cannot be resolved.
     */
    TranslatedQuery translateFromFriendlyName(String query) throws CustomAttributeProcessingException;

    /**
     * Rewrites a {@link ReportResponse} so that any database column keys that were used during the
     * query phase are translated back to their friendly custom attribute names.
     * <p>
     * Use this method paired with {@link #translateFromFriendlyName(String)}. The {@code matchApplied}
     * map should be the one returned by {@link TranslatedQuery#getMatchApplied()} in order to invert
     * the renaming from database column keys back to friendly names in the result set.
     *
     * @param reportResponse the response returned by the analytics query.
     * @param matchApplied the map used during query translation (friendly name -> db column key).
     *                     Keys present in the result set will be replaced with their friendly
     *                     counterparts if a mapping exists.
     * @return a new {@link ReportResponse} instance with keys translated back to friendly names.
     */
    ReportResponse translateResults(ReportResponse reportResponse, Map<String, String> matchApplied);


    /**
     * Value object holding the outcome of a query translation from friendly custom attribute names
     * to database column keys.
     */
    class TranslatedQuery {
        private String translateQuery;
        private Map<String, String> matchApplied;

        /**
         * Creates an instance.
         *
         * @param query the translated query string (typically the Cube.js JSON) after replacing
         *              friendly custom attribute names with database column keys.
         * @param matchApplied a map describing the replacements that were applied where the key is
         *                     the friendly name (e.g., "request.custom.plan") and the value is the
         *                     DB column key (e.g., "request.custom_1"). If {@code null}, an empty map is used.
         */
        public TranslatedQuery(final String query, final Map<String, String> matchApplied) {
            this.translateQuery = query;
            this.matchApplied = matchApplied == null ? new HashMap<>() : matchApplied;
        }

        /**
         * Convenience constructor for when no mapping information is available or no replacements
         * were necessary.
         *
         * @param query the query string to keep as-is.
         */
        public TranslatedQuery(final String query) {
            this(query, new HashMap<>());
        }

        /**
         * Returns the translated query.
         *
         * @return the translated query JSON string.
         */
        public String getTranslateQuery() {
            return translateQuery;
        }

        /**
         * Returns the map of replacements applied during translation.
         *
         * @return a map where the key is the friendly custom attribute name and the value is the
         *         database column key that replaced it.
         */
        public Map<String, String> getMatchApplied() {
            return matchApplied;
        }
    }
}
