package com.dotcms.rest.api.v1.analytics.content.util;

import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ContentAnalyticsUtil#applyUrlParamFilter(Map, Map)}, the step of the LTS event
 * ingest path that cleans the page URL fields of the events before they are sent (private issue #632).
 * The filter rules themselves are covered by {@code UrlParamFilterTest}.
 */
public class ContentAnalyticsUtilUrlFilterTest {

    private static final String URL = "https://site.example.com/a;jsessionid=ABC123?utm_source=news&email=a%40b.com&token=xyz#top";

    /**
     * Given Scenario: an event whose page URL carries a campaign parameter, an email and a token, and a site
     * with no Content Analytics App settings
     * ExpectedResult: only the campaign parameter is kept (an anchor without parameters stays), and the session id and
     * the parameters of the fragment are gone
     */
    @Test
    public void defaultsKeepOnlyCampaignParameters() {

        final Map<String, Object> payload = payload(URL, "?utm_source=news&email=a%40b.com", "#a=1&token=xyz");

        ContentAnalyticsUtil.applyUrlParamFilter(payload, Collections.emptyMap());

        final Map<String, Object> page = page(payload, 0);
        assertEquals("https://site.example.com/a?utm_source=news#top", page.get("url"));
        assertEquals("?utm_source=news", page.get("doc_search"));
        assertEquals("", page.get("doc_hash"));
    }

    /**
     * Given Scenario: the site lists an extra parameter in its Allowed URL Parameters field
     * ExpectedResult: that parameter is kept too
     */
    @Test
    public void siteAllowedParameterIsKept() {

        final Map<String, Secret> secrets = new HashMap<>();
        secrets.put(ContentAnalyticsUtil.ALLOWED_URL_PARAMS_KEY,
                Secret.builder().withType(Type.STRING).withValue("email".toCharArray()).build());
        final Map<String, Object> payload = payload(URL, "", "");

        ContentAnalyticsUtil.applyUrlParamFilter(payload, secrets);

        assertEquals("https://site.example.com/a?utm_source=news&email=a%40b.com#top", page(payload, 0).get("url"));
    }

    /**
     * Given Scenario: the site turned the parameter filter off
     * ExpectedResult: query parameters are kept, but user info and session ids are still removed
     */
    @Test
    public void filterOff_stillRemovesUserInfoAndSessionIds() {

        final Map<String, Secret> secrets = new HashMap<>();
        secrets.put(ContentAnalyticsUtil.FILTER_URL_PARAMS_KEY,
                Secret.builder().withType(Type.BOOL).withValue("false".toCharArray()).build());
        final Map<String, Object> payload = payload("https://user:pw@site.example.com/a;jsessionid=ABC123?token=xyz", "", "");

        ContentAnalyticsUtil.applyUrlParamFilter(payload, secrets);

        assertEquals("https://site.example.com/a?token=xyz", page(payload, 0).get("url"));
    }

    /**
     * Given Scenario: an event whose {@code data.page} is not an object
     * ExpectedResult: that event is removed from the payload and the others are kept
     */
    @Test
    public void malformedEventIsRemoved() {

        final Map<String, Object> payload = payload(URL, "", "");
        final Map<String, Object> bad = new HashMap<>();
        bad.put("data", Collections.singletonMap("page", "not-an-object"));
        events(payload).add(bad);

        ContentAnalyticsUtil.applyUrlParamFilter(payload, Collections.emptyMap());

        assertEquals(1, events(payload).size());
        assertTrue(page(payload, 0).get("url").toString().startsWith("https://site.example.com/a"));
    }

    private static Map<String, Object> payload(final String url, final String docSearch, final String docHash) {

        final Map<String, Object> page = new HashMap<>();
        page.put("url", url);
        page.put("doc_search", docSearch);
        page.put("doc_hash", docHash);
        final Map<String, Object> data = new HashMap<>();
        data.put("page", page);
        final Map<String, Object> event = new HashMap<>();
        event.put("event_type", "pageview");
        event.put("data", data);
        final List<Map<String, Object>> events = new ArrayList<>();
        events.add(event);
        final Map<String, Object> payload = new HashMap<>();
        payload.put("events", events);
        return payload;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> events(final Map<String, Object> payload) {
        return (List<Map<String, Object>>) payload.get("events");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> page(final Map<String, Object> payload, final int index) {
        return (Map<String, Object>) ((Map<String, Object>) events(payload).get(index).get("data")).get("page");
    }
}
