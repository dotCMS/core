package com.dotcms.jitsu;


import com.dotmarketing.util.json.JSONObject;

import java.io.Serializable;
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
                .map(jsonObject -> putContent(jsonObject, newRootContext, sessionId))
                .map(this::putEventAttributes)
                .map(ValidAnalyticsEventPayloadTransformer::removeData)
                .map(EventsPayload.EventPayload::new)
                .collect(Collectors.toList());
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


        final Map<String, Object> utmAttributes = (Map<String, Object> ) dataAttributes.get(UTM_ATTRIBUTE_NAME);
        jsonObject.put(UTM_ATTRIBUTE_NAME, utmAttributes);

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
