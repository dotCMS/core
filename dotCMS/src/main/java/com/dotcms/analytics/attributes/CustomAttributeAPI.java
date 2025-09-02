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

    TranslatedQuery translateFromFriendlyName(String query) throws CustomAttributeProcessingException;

    ReportResponse translateResults(ReportResponse reportResponse, Map<String, String> matchApplied);


    class TranslatedQuery {
        private String translateQuery;
        private Map<String, String> matchApplied;

        public TranslatedQuery(final String query, final Map<String, String> matchApplied) {
            this.translateQuery = query;
            this.matchApplied = matchApplied == null ? new HashMap<>() : matchApplied;
        }

        public TranslatedQuery(final String query) {
            this(query, new HashMap<>());
        }

        public String getTranslateQuery() {
            return translateQuery;
        }

        public Map<String, String> getMatchApplied() {
            return matchApplied;
        }
    }
}
