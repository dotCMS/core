package com.dotcms.jitsu;

import com.dotcms.util.JsonUtil;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ValidAnalyticsEventPayloadTest  {

    private String jsonPayload = "{" +
        "\"context\": {" +
            "\"site_key\": \"xyz\"," +
            "\"session_id\": \"abc\"," +
            "\"user_id\": \"qwe\", " +
            "\"device\": {" +
                "\"screen_resolution\": \"1280x720\"," +
                "\"language\": \"en\"," +
                "\"viewport_width\": \"1280\"," +
                "\"viewport_height\": \"720\"" +
            "}" +
        "}," +
         " \"events\": [" +
            "{" +
                "\"event_type\": \"pageview\"," +
                "\"local_time\": \"2025-06-09T14:30:00+02:00\"," +
                "\"data\": {" +
                    "\"page\": {" +
                        "\"url\": \"http://loquesea.com/index#pepito?a=b\"," +
                        "\"doc_encoding\": \"UTF8\"," +
                        "\"title\": \"This is my index page\"," +
                        "\"languageID\": \"23213\"," +
                        "\"persona\": \"ANY_PERSONA\"," +
                        "\"dot_path\": \"/index\"," +
                        "\"dot_host\": \"loquesea.com\"," +
                        "\"doc_protocol\": \"http\"," +
                        "\"doc_hash\": \"pepito\",\n" +
                        "\"doc_search\": \"a=b\"," +
                        "\"referer\": \"referer\"," +
                        "\"user_agent\": \"useragent=b\"" +
                    "}," +
                    "\"utm\": {" +
                        "\"medium\": \"medium\"," +
                        "\"source\": \"source\"," +
                        "\"campaign\": \"campaign\"," +
                        "\"term\": \"term\"," +
                        "\"content\": \"content\"" +
                    "}" +
                "}" +
            "}," +
            "{" +
                "\"event_type\": \"pageview\"," +
                "\"local_time\": \"2025-06-09T14:30:00+02:00\"," +
                "\"data\": {" +
                    "\"page\": {" +
                        "\"url\": \"http://loquesea.com/another_page#pepe?c=d\"," +
                        "\"doc_encoding\": \"UTF8\"," +
                        "\"title\": \"This is my another page\"," +
                        "\"languageID\": \"555555\"," +
                        "\"persona\": \"ANY_PERSONA_BUT_NOT_PREVIOUS_PERSONA\"," +
                        "\"dot_path\": \"/another_page\"," +
                        "\"dot_host\": \"loquesea.com\"," +
                        "\"doc_protocol\": \"http\"," +
                        "\"doc_hash\": \"pepe\",\n" +
                        "\"doc_search\": \"c=d\"," +
                        "\"referer\": \"another_referer\"," +
                        "\"user_agent\": \"another_useragent=b\"" +
                    "}," +
                    "\"device\": {" +
                        "\"screen_resolution\": \"3840x2160\"," +
                        "\"language\": \"en\"," +
                        "\"viewport_width\": \"3840\"," +
                        "\"viewport_height\": \"2160\"" +
                     "}," +
                    "\"utm\": {" +
                        "\"medium\": \"another_medium\"," +
                        "\"source\": \"another_source\"," +
                        "\"campaign\": \"another_campaign\"," +
                        "\"term\": \"another_term\"," +
                        "\"content\": \"another_content\"" +
                    "}" +
                "}" +
            "}" +
         "]" +
    "}";

    /**
     * Method to test: {@link ValidAnalyticsEventPayload#payloads()}
     * when: a {@link ValidAnalyticsEventPayload} is created with a payload with sevarals events
     * should: return a EventPayload for each event
     */
    @Test
    public void payloads() throws IOException {

        Map<String, Object> payload = JsonUtil.getJsonFromString(jsonPayload);

        final ValidAnalyticsEventPayload validAnalyticsEventPayload = new ValidAnalyticsEventPayload(payload);

        assertEquals(2, ((List) validAnalyticsEventPayload.payloads()).size());
    }

    /**
     * Method to test: {@link ValidAnalyticsEventPayload#payloads()}
     * when: a {@link ValidAnalyticsEventPayload} is created with a payload with several events
     * should: return a EventPayload for each event
     */
    @Test
    public void transform() throws IOException {
        Map<String, Object> payloadMap = JsonUtil.getJsonFromString(jsonPayload);
        List<Map<String, Object>> events = (List<Map<String, Object>> ) payloadMap.get("events");
        Map<String, Object> expectedContext = (Map<String, Object>) payloadMap.get("context");

        final ValidAnalyticsEventPayload validAnalyticsEventPayload = new ValidAnalyticsEventPayload(payloadMap);
        int i = 0;

        for (EventsPayload.EventPayload payload : validAnalyticsEventPayload.payloads()) {
            final Map<String, Object> context = (Map<String, Object>) payload.get("context");
            assertNotNull(context);
            assertEquals(2, context.size());
            assertEquals("xyz", context.get("site_key"));
            assertEquals("qwe", context.get("user_id"));

            assertEquals("abc", payload.get("sessionid"));
            assertEquals(events.get(i).get("event_type"), payload.get("event_type"));
            assertEquals("2025-06-09T12:30:00.000000Z", payload.get("utc_time"));

            Map<String, Object> dataAttributes = (Map<String, Object>) events.get(i).get("data");
            checkAttributes(payload, dataAttributes, "page", Map.of("title", "page_title", "language", "userlanguage"));
            checkAttributes(payload, expectedContext, "device", Map.of("language", "user_language"));


            Map<String, Object> utmAttributesExpected = (Map<String, Object>)  dataAttributes.get("utm");
            Map<String, Object> utmPayload = (Map<String, Object>)  payload.get("utm");

            for (Map.Entry<String, Object> utmAttributesEntry : utmAttributesExpected.entrySet()) {
                assertEquals(utmAttributesEntry.getValue(), utmPayload.get(utmAttributesEntry.getKey()));
            }

            assertNull(payload.get("data"));
            assertNull(payload.get("title"));
            assertNull(payload.get("language"));
            i++;
        }

    }

    private static void checkAttributes(final EventsPayload.EventPayload payload,
                                        final Map<String, Object> dataAttributes,
                                        final String key,
                                        final Map<String, String> replacementKeys) {
        final Map<String, Object> attributes = (Map<String, Object>)  dataAttributes.get(key);

        checkAttributes(payload, attributes, replacementKeys);
    }

    private static void checkAttributes(final EventsPayload.EventPayload payload,
                                        final Map<String, Object> attributes,
                                        final Map<String, String> replacementKeys) {

        for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
            final String key = replacementKeys.containsKey(attribute.getKey()) ?
                    replacementKeys.get(attribute.getKey()) : attribute.getKey();

            assertEquals(String.format("The value %s is not expected for %s", payload.get(key), key),
                    attribute.getValue(), payload.get(key));
        }
    }
}