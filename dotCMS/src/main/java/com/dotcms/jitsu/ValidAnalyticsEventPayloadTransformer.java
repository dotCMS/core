package com.dotcms.jitsu;


import com.dotcms.analytics.attributes.CustomAttributeAPIImpl;
import com.dotcms.analytics.metrics.EventType;
import com.dotcms.jitsu.validators.AnalyticsValidatorUtil;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dotcms.jitsu.ValidAnalyticsEventPayloadAttributes.*;

/**
 * A class responsible for applying any necessary transformations to the analytics events payload
 * before it is sent to the Jitsu server.
 */
public enum ValidAnalyticsEventPayloadTransformer {
    INSTANCE;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");

    /**
     * Apply any necessary transformations to the analytics events payload.
     *
     * <p><strong>Incoming payload example:</strong></p>
     *
     * <pre>
     *  {
     *   "context": {
     *     "siteKey": "xyz",
     *     "sessionId": "abc"
     *     "userId": "abc"
     *   },
     *   "events": [
     *     {
     *       "eventType": "page_view",
     *       "data": {
     *           "page": {
     *               "url": "http://loquesea.com/index#pepito?a=b",
     *               "encoding": "UTF8",
     *               "title": "This is my index page",
     *               "languageID": "23213",
     *               "persona": "ANY_PERSONA"
     *               "dot_path": "...",
     *               "dot_host": "...",
     *               "doc_protocol": "...",
     *               "doc_hash": "...",
     *               "doc_search": "...",
     *               "referer": "...",
     *               "useragent": "..."
     *           },
     *           "device": {
     *             "screenResolution": "1280x720",
     *             "language": "en",
     *             "viewportWidth": "..." ,
     *             "viewportHeight": "..."
     *           },
     *           "utm": {
     *             "medium": "...",
     *             "source": "...",
     *             "campaign": "...",
     *             "term": "...",
     *             "content": "..."
     *           }
     *       }
     *     }
     *   ]
     * }
     * </pre>
     *
     * This class makes the follows changes:
     *
     * - Move sessionId out of context
     * - Move each attribute in the page section out pf page and data
     * - Move each attribute in the device section out pf device and data
     * - Move utm section out of data
     *
     * @param payload Analytics event payload
     */
    public Collection<EventsPayload.EventPayload> transform(final JSONObject payload){
        final Map<String, Serializable> newRootContext =
                (Map<String, Serializable>) payload.get(CONTEXT_ATTRIBUTE_NAME);

        final Serializable sessionId = newRootContext.get(SESSION_ID_ATTRIBUTE_NAME);
        newRootContext.remove(SESSION_ID_ATTRIBUTE_NAME);

        final List<Map<String, Serializable>> events =
                (List<Map<String, Serializable>>) payload.get(EVENTS_ATTRIBUTE_NAME);

        return events.stream()
                .map(JSONObject::new)
                .map(ValidAnalyticsEventPayloadTransformer::transformDate)
                .map(jsonObject -> ValidAnalyticsEventPayloadTransformer.setRootValues(jsonObject, payload))
                .map(jsonObject -> putContent(jsonObject, newRootContext, sessionId))
                .map(this::putEventAttributes)
                .map(this::transformCustom)
                .map(ValidAnalyticsEventPayloadTransformer::removeData)
                .map(EventsPayload.EventPayload::new)
                .collect(Collectors.toList());
    }

    /**
     * Translates the "custom" section inside the event's data into top-level database-ready fields.
     * <p>
     * If the event contains data.custom, this method will:
     * - Parse the custom object into a Map
     * - Use the Analytics Custom Attribute API to translate human-friendly keys to database column names
     * - Merge the translated key/value pairs into the event JSON at the root level
     *
     * Any I/O parsing issues are wrapped in a RuntimeException.
     *
     * @param event The event JSON object that may contain data.custom
     * @return The same event JSON object with translated custom attributes merged at the root level
     */
    private JSONObject transformCustom(final JSONObject event) {
        final String eventType = event.optString(EVENT_TYPE_ATTRIBUTE_NAME);
        final JSONObject data = event.getJSONObject(DATA_ATTRIBUTE_NAME);

        Logger.debug(ValidAnalyticsEventPayloadTransformer.class, () -> "transformCustom invoked for eventType='" + eventType + "' - has 'custom': " + data.has(CUSTOM_ATTRIBUTE_NAME));

        try {
            if (data.has(CUSTOM_ATTRIBUTE_NAME)) {
                final JSONObject customJson = data.getJSONObject(ValidAnalyticsEventPayloadAttributes.CUSTOM_ATTRIBUTE_NAME);
                final Map<String, Object> jsonAsMap = JsonUtil.getJsonFromString(customJson.toString());
                Logger.debug(ValidAnalyticsEventPayloadTransformer.class, () -> "Parsed 'custom' map for eventType='" + eventType + "' with " + (jsonAsMap != null ? jsonAsMap.size() : 0) + " attribute(s)");

                Logger.debug(ValidAnalyticsEventPayloadTransformer.class, () -> "Translating 'custom' attributes to DB columns for eventType='" + eventType + "'");
                Map<String, Object> customTranslated = APILocator.getAnalyticsCustomAttribute()
                        .translateToDatabase(eventType, jsonAsMap);

                Logger.debug(ValidAnalyticsEventPayloadTransformer.class, () -> "Translation complete for eventType='" + eventType + "'. Translated " + (customTranslated != null ? customTranslated.size() : 0) + " attribute(s): " + (customTranslated != null ? customTranslated.keySet() : java.util.Collections.emptySet()));

                event.putAll(customTranslated);
            }
        } catch (IOException e) {
            Logger.error(ValidAnalyticsEventPayloadTransformer.class, e.getMessage(), e);
            throw new RuntimeException(e);
        }

        return event;
    }

    /**
     * Copies primitive values from the root payload to the event JSON object.
     * Only copies values that are not JSONObject or JSONArray instances.
     *
     * @param jsonEVent The event JSON object to which root values will be added
     * @param rootPayload The root payload containing values to be copied
     * @return The modified event JSON object with added root values
     */
    private static JSONObject setRootValues(final JSONObject jsonEVent, final JSONObject rootPayload) {
        for (Object key : rootPayload.keySet()) {
            Object value = rootPayload.get(key);

            if (!(value instanceof JSONObject) && !(value instanceof JSONArray)) {
                jsonEVent.put(key, value);
            }
        }

        return jsonEVent;
    }

    /**
     * Removes the "data" section from the payload.
     * After moving its attributes outside of the "data" section, it is no longer needed.
     *
     * @param payload Event's payload
     * @return
     */
    private static JSONObject removeData(final JSONObject payload) {
        payload.remove(DATA_ATTRIBUTE_NAME);
        return payload;
    }

    private static JSONObject transformDate(final JSONObject payload) {
        final String eventType = payload.get(EVENT_TYPE_ATTRIBUTE_NAME).toString();
        final List<String> dateFields = AnalyticsValidatorUtil.INSTANCE.getDateField(eventType);

        for (String dateField : dateFields) {
            final Object dateValue = getValueFromPath(payload, dateField);

            if (dateValue != null && dateValue instanceof String) {
                final String utcDate = transformToUTC((String) dateValue);
                updateValueInPath(payload, dateField, utcDate);
            }
        }

        return payload;
    }

    /**
     * Gets a value from a JSON object using a dot-notation path.
     *
     * @param jsonObject The JSON object to get the value from
     * @param path The path to the value, using dot notation (e.g., "data.local_time")
     * @return The value at the path, or null if the path doesn't exist
     */
    private static Object getValueFromPath(final JSONObject jsonObject, final String path) {
        String[] parts = path.split("\\.");
        JSONObject current = jsonObject;

        // Navigate through the path
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i])) {
                return null;
            }
            Object value = current.opt(parts[i]);
            if (!(value instanceof JSONObject)) {
                return null;
            }
            current = (JSONObject) value;
        }

        // Get the final value
        String lastPart = parts[parts.length - 1];
        if (!current.has(lastPart)) {
            return null;
        }
        return current.opt(lastPart);
    }

    /**
     * Updates a value in a JSON object using a dot-notation path.
     *
     * @param jsonObject The JSON object to update
     * @param path The path to the value, using dot notation (e.g., "data.local_time")
     * @param newValue The new value to set
     */
    private static void updateValueInPath(final JSONObject jsonObject, final String path, final Object newValue) {
        String[] parts = path.split("\\.");
        JSONObject current = jsonObject;

        // Navigate through the path
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i])) {
                // Create missing objects along the path
                current.put(parts[i], new JSONObject());
            }
            Object value = current.opt(parts[i]);
            if (!(value instanceof JSONObject)) {
                // Replace non-object values with objects
                current.put(parts[i], new JSONObject());
                value = current.opt(parts[i]);
            }
            current = (JSONObject) value;
        }

        // Set the final value
        String lastPart = parts[parts.length - 1];
        current.put(lastPart, newValue);
    }

    private static String transformToUTC(final String dateValue) {
        OffsetDateTime dateTimeWithOffset = OffsetDateTime.parse(dateValue);
        OffsetDateTime utcDateTime = dateTimeWithOffset.withOffsetSameInstant(ZoneOffset.UTC);

        return  utcDateTime.format(DATE_FORMATTER);
    }

    /**
     * This method is in charge of:
     *
     * - Move sessionId out of context
     * - Move each attribute in the page section out pf page and data
     * - Move each attribute in the device section out pf device and data
     * - Move utm section out of data
     *
     * @param jsonObject
     * @return
     */
    private JSONObject putEventAttributes(final JSONObject jsonObject) {
        final Map<String, Object> dataAttributes = (Map<String, Object> ) jsonObject.get(DATA_ATTRIBUTE_NAME);
        final Map<String, Object> pageAttributes = (Map<String, Object> ) dataAttributes.get(PAGE_ATTRIBUTE_NAME);
        final Map<String, Object> deviceAttributes = (Map<String, Object> ) dataAttributes.get(DEVICE_ATTRIBUTE_NAME);

        moveToRoot(jsonObject, pageAttributes,
                Map.of("title", "page_title", "language_id", "userlanguage"));

        moveToRoot(jsonObject, deviceAttributes, Map.of("language", "user_language"));

        if (dataAttributes.containsKey(UTM_ATTRIBUTE_NAME)) {
            final Map<String, Object> utmAttributes = (Map<String, Object>) dataAttributes.get(UTM_ATTRIBUTE_NAME);
            jsonObject.put(UTM_ATTRIBUTE_NAME, utmAttributes);
        }

        final String localTimeAttributes = (String) jsonObject.get(LOCAL_TIME_ATTRIBUTE_NAME);
        jsonObject.put("utc_time", localTimeAttributes);
        jsonObject.remove(LOCAL_TIME_ATTRIBUTE_NAME);

        return jsonObject;
    }

    /**
     * Moves a set of attributes from a nested object to the root level of the JSON object.
     *
     * For example, given the following input:
     *
     * <pre>
     * {
     *   "data": {
     *     "page": {
     *       "url": "http://loquesea.com/index#pepito?a=b",
     *       "encoding": "UTF8",
     *       "title": "This is my index page",
     *       "languageID": "23213",
     *       "persona": "ANY_PERSONA"
     *     }
     *   }
     * }
     * </pre>
     *
     * If you pass the contents of the "page" object to this method, the resulting output will be:
     *
     * <pre>
     * {
     *   "url": "http://loquesea.com/index#pepito?a=b",
     *   "encoding": "UTF8",
     *   "title": "This is my index page",
     *   "languageID": "23213",
     *   "persona": "ANY_PERSONA"
     * }
     * </pre>
     *
     * Additionally, if you specify replacements such as:
     * <ul>
     *   <li><code>persona -> newPersona</code></li>
     *   <li><code>title -> newTitle</code></li>
     * </ul>
     * The final result will be:
     *
     * <pre>
     * {
     *   "url": "http://loquesea.com/index#pepito?a=b",
     *   "encoding": "UTF8",
     *   "newTitle": "This is my index page",
     *   "languageID": "23213",
     *   "newPersona": "ANY_PERSONA"
     * }
     * </pre>
     *
     * @param jsonObject Json Object
     * @param attributes attributes to move to the root
     * @param replacementsKeys Key that you want to replace when theay are move.
     */
    private static void moveToRoot(final JSONObject jsonObject,
                                   final Map<String, Object> attributes,
                                   final Map<String, String> replacementsKeys) {

        if (!UtilMethods.isSet(attributes)) {
            return;
        }

        for (Map.Entry<String, Object> attributeEntry : attributes.entrySet()) {
            final String attributeKey = replacementsKeys.containsKey(attributeEntry.getKey()) ?
                    replacementsKeys.get(attributeEntry.getKey()) : attributeEntry.getKey();
            jsonObject.put(attributeKey, attributeEntry.getValue());
        }
    }

    /**
     * This copy context in each EventPayload
     *
     * @param eventJsonObject
     * @param context
     * @param sessionId
     * @return
     */
    private static JSONObject putContent(final JSONObject eventJsonObject,
                                         final Map<String, Serializable> context,
                                         final Serializable sessionId) {

        eventJsonObject.put(CONTEXT_ATTRIBUTE_NAME, context);
        eventJsonObject.put(SESSION_ID_JITSU_ATTRIBUTE_NAME, sessionId);

        return eventJsonObject;
    }
}
