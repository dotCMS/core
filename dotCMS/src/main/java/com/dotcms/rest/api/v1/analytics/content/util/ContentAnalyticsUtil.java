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
import com.dotcms.jitsu.ValidAnalyticsEventPayload;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UUIDUtil;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ContentAnalyticsUtil {

    private static final EventLogSubmitter SUBMITTER  = new EventLogSubmitter();
    private static final UserCustomDefinedRequestMatcher USER_CUSTOM_DEFINED_REQUEST_MATCHER =  new UserCustomDefinedRequestMatcher();

    private static final Map<String, Supplier<RequestMatcher>> MATCHER_MAP = Map.of(
            EventType.FILE_REQUEST.getType(), FilesRequestMatcher::new,
            EventType.PAGE_REQUEST.getType(), PagesAndUrlMapsRequestMatcher::new,
            EventType.URL_MAP.getType(), PagesAndUrlMapsRequestMatcher::new,
            EventType.VANITY_REQUEST.getType(), VanitiesRequestMatcher::new
    );

    public static void registerContentAnalyticsRestEvent(HttpServletRequest request, HttpServletResponse response,
            Map<String, Serializable> userEventPayload) {

        final String requestId = Objects.nonNull(request.getAttribute("requestId")) ?
                request.getAttribute("requestId").toString() : UUIDUtil.uuid();
        request.setAttribute("requestId", requestId);

        final Map<String, Serializable> userEventPayloadWithDefaults = new HashMap<>(
                userEventPayload);
        userEventPayloadWithDefaults.put(Collector.EVENT_SOURCE, EventSource.REST_API.getName());
        if (!userEventPayloadWithDefaults.containsKey(Collector.EVENT_TYPE)){
            userEventPayloadWithDefaults.put(Collector.EVENT_TYPE,   userEventPayload
                    .getOrDefault(Collector.EVENT_TYPE, EventType.CUSTOM_USER_EVENT.getType()));
        }

        userEventPayload.put("url", "");
        userEventPayload.put("request_id", requestId);

        try {
            final Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
            SUBMITTER.logEvent(host, new ValidAnalyticsEventPayload((Map<String, Object>) (Map<?, ?>) userEventPayload));
        } catch (DotDataException | DotSecurityException | PortalException | SystemException e) {
            throw new RuntimeException(e);
        }
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
