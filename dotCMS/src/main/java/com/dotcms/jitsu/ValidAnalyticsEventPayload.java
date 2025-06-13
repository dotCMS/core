package com.dotcms.jitsu;


import com.dotmarketing.util.json.JSONObject;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dotcms.jitsu.ValidAnalyticsEventPayloadAttributes.*;

/**
 * Processes incoming analytics event data by performing validation and transformation
 * before it is passed to the downstream systems (Jitsu).
 * <p>
 * This class acts as an intermediary layer between raw event payloads (e.g., {@code AnalyticsEventsPayload})
 * and the core analytics system. It ensures that the event data is clean, consistent,
 * and conforms to expected formats and structures.
 * </p>
 *
 * Responsibilities:
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
 * This class is intended to eventually replace {@code AnalyticsEventsPayload}
 */
public class ValidAnalyticsEventPayload extends EventsPayload {


    public ValidAnalyticsEventPayload(Map<String, Object> payload) {
        super(payload);
    }

    /**
     * Returns the collection of {@link EventsPayload} instances that will be sent to Jitsu.
     *
     * The expected JSON payload structure is:
     *
     * <pre>
     * {
     *   "context": {
     *     ...
     *   },
     *   "events": [
     *     ...
     *   ]
     * }
     * </pre>
     *
     * For each item in the "events" array, an {@link EventsPayload} object is created.
     * The "context" attributes are copied into each of these instances.
     *
     * @return a collection of {@link EventsPayload} objects to be sent to Jitsu.
     */
    @Override
    public Iterable<EventPayload> payloads() {
        return ValidAnalyticsEventPayloadTransformer.INSTANCE.transform(jsonObject);
    }
}
