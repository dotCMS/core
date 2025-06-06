package com.dotcms.rest.api.v1.analytics.content.util;

import com.dotcms.analytics.track.collectors.Collector;
import com.dotcms.analytics.track.collectors.EventSource;
import com.dotcms.analytics.track.collectors.EventType;
import com.dotcms.analytics.track.collectors.WebEventsCollectorServiceFactory;
import com.dotcms.analytics.track.matchers.FilesRequestMatcher;
import com.dotcms.analytics.track.matchers.PagesAndUrlMapsRequestMatcher;
import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotcms.analytics.track.matchers.UserCustomDefinedRequestMatcher;
import com.dotcms.analytics.track.matchers.VanitiesRequestMatcher;
import com.dotcms.jitsu.EventLogSubmitter;
import com.dotcms.jitsu.EventsPayload;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ContentAnalyticsUtil {

    public static final Map<String, String> CUSTOM_MATCH = new HashMap<>();

    private static final UserCustomDefinedRequestMatcher USER_CUSTOM_DEFINED_REQUEST_MATCHER =  new UserCustomDefinedRequestMatcher();
    private static final EventLogSubmitter SUBMITTER  = new EventLogSubmitter();;

    private static final Map<String, Supplier<RequestMatcher>> MATCHER_MAP = Map.of(
            EventType.FILE_REQUEST.getType(), FilesRequestMatcher::new,
            EventType.PAGE_REQUEST.getType(), PagesAndUrlMapsRequestMatcher::new,
            EventType.URL_MAP.getType(), PagesAndUrlMapsRequestMatcher::new,
            EventType.VANITY_REQUEST.getType(), VanitiesRequestMatcher::new
    );

    public static void registerContentAnalyticsRestEvent(HttpServletRequest request, HttpServletResponse response,
            Map<String, Serializable> userEventPayload) {

        final String requestId = Objects.nonNull(request.getAttribute("requestId")) ?
                (String) request.getAttribute("requestId") : UUIDUtil.uuid();
        request.setAttribute("requestId", requestId);

        final Map<String, Serializable> userEventPayloadWithDefaults = new HashMap<>(
                userEventPayload);

        userEventPayloadWithDefaults.put(Collector.EVENT_SOURCE, EventSource.REST_API.getName());

        if (!userEventPayloadWithDefaults.containsKey(Collector.EVENT_TYPE)){
            userEventPayloadWithDefaults.put(Collector.EVENT_TYPE,   userEventPayload.getOrDefault(Collector.EVENT_TYPE, EventType.CUSTOM_USER_EVENT.getType()));
        }

        final String eventType = userEventPayloadWithDefaults.get(Collector.EVENT_TYPE).toString();

        final Map<String, String> customAttributes = (Map<String, String>) userEventPayload.get("custom");
        final Map<String, String> customAttributesTranslate = new HashMap<>();

        for (Map.Entry<String, String> customAttribute : customAttributes.entrySet()) {
            String match = CUSTOM_MATCH.get(customAttribute.getKey());

            if (match == null) {

                if (CUSTOM_MATCH.size() >= 10) {
                    throw new IllegalArgumentException(
                            String.format("Max limit of custom attributes to the event %s reach", eventType));
                }

                match = "custom_" + CUSTOM_MATCH.size();
                CUSTOM_MATCH.put(customAttribute.getKey(), match);
            }

            customAttributesTranslate.put(match, customAttribute.getValue());
        }

        userEventPayload.remove("custom");
        userEventPayload.putAll(customAttributesTranslate);
        userEventPayload.put("url", "");
        userEventPayload.put("request_id", requestId);

        try {
            final Host  host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
            SUBMITTER.logEvent(host, new NewEventsPayload((Map<String, Object>) (Map<?, ?>) userEventPayload));
        } catch (DotDataException | DotSecurityException | PortalException | SystemException e) {
            throw new RuntimeException(e);
        }


        /*WebEventsCollectorServiceFactory.getInstance().getWebEventsCollectorService().fireCollectorsAndEmitEvent(
                request, response,
                loadRequestMatcher(userEventPayload), userEventPayloadWithDefaults, fromPayload(
                        userEventPayload));*/
    }

    private static Map<String, Object> fromPayload(final Map<String, Serializable> userEventPayload) {
        final Map<String, Object> baseContextMap = new HashMap<>();

        if (userEventPayload.containsKey("url")) {

            baseContextMap.put("uri", userEventPayload.get("url"));
        }

        if (userEventPayload.containsKey("doc_path")) {

            baseContextMap.put("uri", userEventPayload.get("doc_path"));
        }

        return baseContextMap;
    }

    private static RequestMatcher loadRequestMatcher(final Map<String, Serializable> userEventPayload) {

        String eventType = (String) userEventPayload.getOrDefault(Collector.EVENT_TYPE, EventType.CUSTOM_USER_EVENT.getType());
        return MATCHER_MAP.getOrDefault(eventType, () -> USER_CUSTOM_DEFINED_REQUEST_MATCHER).get();
    }

    private static class NewEventsPayload extends EventsPayload {

        private Map<String, Object> payload;

        public NewEventsPayload(Map<String, Object> payload) {
            super(payload);
            this.payload = payload;
        }

        public Iterable<EventPayload> payloads() {

            try {
                final JSONObject experimentJsonPayload = new JSONObject(JsonUtil.getJsonAsString(payload));
                return Arrays.asList(new EventPayload(experimentJsonPayload));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
