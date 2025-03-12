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
import com.dotmarketing.util.UUIDUtil;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ContentAnalyticsUtil {

    private static final UserCustomDefinedRequestMatcher USER_CUSTOM_DEFINED_REQUEST_MATCHER =  new UserCustomDefinedRequestMatcher();

    private static final Map<String, Supplier<RequestMatcher>> MATCHER_MAP = Map.of(
            EventType.FILE_REQUEST.getType(), FilesRequestMatcher::new,
            EventType.PAGE_REQUEST.getType(), PagesAndUrlMapsRequestMatcher::new,
            EventType.URL_MAP.getType(), PagesAndUrlMapsRequestMatcher::new,
            EventType.VANITY_REQUEST.getType(), VanitiesRequestMatcher::new
    );

    public static void registerContentAnalyticsRestEvent(HttpServletRequest request, HttpServletResponse response,
            Map<String, Serializable> userEventPayload) {

        request.setAttribute("requestId", Objects.nonNull(request.getAttribute("requestId")) ? request.getAttribute("requestId") : UUIDUtil.uuid());

        final Map<String, Serializable> userEventPayloadWithDefaults = new HashMap<>(
                userEventPayload);
        userEventPayloadWithDefaults.put(Collector.EVENT_SOURCE, EventSource.REST_API.getName());
        if (!userEventPayloadWithDefaults.containsKey(Collector.EVENT_TYPE)){
            userEventPayloadWithDefaults.put(Collector.EVENT_TYPE,   userEventPayload.getOrDefault(Collector.EVENT_TYPE, EventType.CUSTOM_USER_EVENT.getType()));
        }
        WebEventsCollectorServiceFactory.getInstance().getWebEventsCollectorService().fireCollectorsAndEmitEvent(
                request, response,
                loadRequestMatcher(userEventPayload), userEventPayloadWithDefaults, fromPayload(
                        userEventPayload));
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

}
