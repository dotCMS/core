package com.dotcms.analytics.queue;

import com.dotcms.analytics.track.collectors.Collector;
import com.dotcms.util.JsonUtil;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transforms server-side collector payloads ({@code List<Map<String, Serializable>>}) into JSON
 * that matches the {@code UserEventPayload} schema expected by the event manager's
 * {@code POST /v1/event/ingest} endpoint.
 *
 * <p>Collector payloads are flat maps with keys defined in {@link Collector}. The event manager
 * expects a nested structure: {@code {"context": {...}, "events": [{...}]}}.
 */
public final class CollectorPayloadTransformer {

    private static final Map<String, String> EVENT_TYPE_MAP = Map.of(
            "PAGE_REQUEST", "pageview",
            "FILE_REQUEST", "pageview",
            "URL_MAP", "pageview",
            "VANITY_REQUEST", "pageview",
            "CUSTOM_USER_EVENT", "pageview"
    );

    private CollectorPayloadTransformer() {}

    /**
     * Transforms a list of collector payload maps into event-manager-compatible JSON.
     *
     * @param payloads collector output — one map per event
     * @return JSON string matching {@code UserEventPayload} schema, or {@code null} if empty
     */
    @Nullable
    public static String toJson(final List<Map<String, Serializable>> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            return null;
        }

        final Map<String, Serializable> first = payloads.get(0);
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put("context", buildContext(first));
        result.put("events", buildEvents(payloads));
        return JsonUtil.getJsonStringFromObject(result);
    }

    private static Map<String, Object> buildContext(final Map<String, Serializable> payload) {
        final Map<String, Object> context = new LinkedHashMap<>();
        context.put("session_id", str(payload, Collector.SESSION_ID));
        context.put("site_id", str(payload, Collector.SITE_ID));
        context.put("user_id", extractUserId(payload));

        final Map<String, Object> device = new LinkedHashMap<>();
        device.put("screen_resolution", "");
        device.put("language", str(payload, Collector.LANGUAGE));
        device.put("viewport_width", "");
        device.put("viewport_height", "");
        context.put("device", device);

        return context;
    }

    private static List<Map<String, Object>> buildEvents(
            final List<Map<String, Serializable>> payloads) {
        final List<Map<String, Object>> events = new ArrayList<>();
        for (final Map<String, Serializable> payload : payloads) {
            events.add(buildEvent(payload));
        }
        return events;
    }

    private static Map<String, Object> buildEvent(final Map<String, Serializable> payload) {
        final String collectorType = str(payload, Collector.EVENT_TYPE);
        final String eventType = EVENT_TYPE_MAP.getOrDefault(collectorType, "pageview");

        final Map<String, Object> event = new LinkedHashMap<>();
        event.put("event_type", eventType);
        event.put("local_time", str(payload, Collector.UTC_TIME));
        event.put("data", buildEventData(payload));
        return event;
    }

    private static Map<String, Object> buildEventData(final Map<String, Serializable> payload) {
        final Map<String, Object> data = new LinkedHashMap<>();
        data.put("page", buildPageData(payload));

        final Map<String, Object> content = buildContentData(payload);
        if (!content.isEmpty()) {
            data.put("content", content);
        }
        return data;
    }

    private static Map<String, Object> buildPageData(final Map<String, Serializable> payload) {
        final Map<String, Object> page = new LinkedHashMap<>();
        final String url = str(payload, Collector.URL);
        page.put("url", url);
        page.put("doc_encoding", "UTF-8");
        page.put("doc_host", str(payload, Collector.SITE_NAME));
        page.put("doc_path", url);

        final Map<String, String> objectMap = objectMap(payload);
        if (objectMap != null) {
            page.put("title", objectMap.getOrDefault(Collector.TITLE, ""));
        }
        return page;
    }

    private static Map<String, Object> buildContentData(final Map<String, Serializable> payload) {
        final Map<String, String> objectMap = objectMap(payload);
        if (objectMap == null) {
            return Map.of();
        }

        final String identifier = objectMap.getOrDefault(Collector.ID, "");
        if (identifier.isEmpty()) {
            return Map.of();
        }

        final Map<String, Object> content = new LinkedHashMap<>();
        content.put("identifier", identifier);
        content.put("inode", objectMap.getOrDefault("inode", ""));
        content.put("title", objectMap.getOrDefault(Collector.TITLE, ""));
        content.put("content_type", objectMap.getOrDefault(Collector.CONTENT_TYPE_VAR_NAME, ""));
        return content;
    }

    private static String extractUserId(final Map<String, Serializable> payload) {
        final Object userObj = payload.get(Collector.USER_OBJECT);
        if (userObj instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> userMap = (Map<String, Object>) userObj;
            final Object id = userMap.get(Collector.ID);
            if (id != null) {
                return id.toString();
            }
        }
        return "anonymous";
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static Map<String, String> objectMap(final Map<String, Serializable> payload) {
        final Object obj = payload.get(Collector.OBJECT);
        return obj instanceof Map ? (Map<String, String>) obj : null;
    }

    private static String str(final Map<String, Serializable> map, final String key) {
        final Object value = map.get(key);
        return value != null ? value.toString() : "";
    }
}
