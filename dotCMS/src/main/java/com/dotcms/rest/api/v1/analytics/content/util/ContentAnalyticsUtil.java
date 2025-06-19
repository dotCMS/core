package com.dotcms.rest.api.v1.analytics.content.util;

import com.dotcms.analytics.track.collectors.Collector;
import com.dotcms.analytics.track.collectors.EventSource;
import com.dotcms.analytics.track.collectors.EventType;
import com.dotcms.analytics.track.matchers.FilesRequestMatcher;
import com.dotcms.analytics.track.matchers.PagesAndUrlMapsRequestMatcher;
import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotcms.analytics.track.matchers.UserCustomDefinedRequestMatcher;
import com.dotcms.analytics.track.matchers.VanitiesRequestMatcher;
import com.dotcms.jitsu.EventLogSubmitter;
import com.dotcms.jitsu.ValidAnalyticsEventPayload;
import com.dotcms.jitsu.ValidAnalyticsEventPayloadAttributes;
import com.dotcms.jitsu.validators.AnalyticsValidatorUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.dotcms.jitsu.ValidAnalyticsEventPayloadAttributes.*;

public class ContentAnalyticsUtil {

    private static final AnalyticsValidatorUtil analyticsValidatorUtil = new AnalyticsValidatorUtil();

    private static final EventLogSubmitter SUBMITTER  = new EventLogSubmitter();
    private static final UserCustomDefinedRequestMatcher USER_CUSTOM_DEFINED_REQUEST_MATCHER =  new UserCustomDefinedRequestMatcher();

    private static final Map<String, Supplier<RequestMatcher>> MATCHER_MAP = Map.of(
            EventType.FILE_REQUEST.getType(), FilesRequestMatcher::new,
            EventType.PAGE_REQUEST.getType(), PagesAndUrlMapsRequestMatcher::new,
            EventType.URL_MAP.getType(), PagesAndUrlMapsRequestMatcher::new,
            EventType.VANITY_REQUEST.getType(), VanitiesRequestMatcher::new
    );

    public static AnalyticsEventsResult registerContentAnalyticsRestEvent(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Map<String, Serializable> userEventPayload) {

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

        final List<AnalyticsValidatorUtil.Error> globalErrors = analyticsValidatorUtil
                .validateGlobalContext(new JSONObject(userEventPayload));

        if (!globalErrors.isEmpty()) {
            return new AnalyticsEventsResult.Builder()
                    .addGlobalErrors(globalErrors)
                    .build();
        }

        final List<Map<String, Object>> payloadEvents = (List<Map<String, Object>>) userEventPayload
                .get(ValidAnalyticsEventPayloadAttributes.EVENTS_ATTRIBUTE_NAME);
        int totalEvents = payloadEvents.size();

        final JSONArray jsonEvents = new JSONArray(payloadEvents);
        final List<AnalyticsValidatorUtil.Error> eventsErrors = analyticsValidatorUtil.validateEvents(jsonEvents);

        removeUnValidPayloads(payloadEvents, eventsErrors);

        if (!payloadEvents.isEmpty()) {
            includeAutomaticallyFields(userEventPayload, request);
            sendEvents(request, userEventPayload, eventsErrors);
        }

        return new AnalyticsEventsResult.Builder()
                .addEventsErrors(eventsErrors)
                .addTotalEvents(totalEvents)
                .build();
    }

    private static void includeAutomaticallyFields(final Map<String, Serializable> userEventPayload,
                                                   final HttpServletRequest request) {
        final String requestId = request.getAttribute("requestId").toString();

        userEventPayload.put("request_id", requestId);

        final String referer = request.getHeader("Referer");
        if (UtilMethods.isSet(referer)) {
            userEventPayload.put(REFERER_ATTRIBUTE_NAME, referer);
        }

        final String userAGent = request.getHeader("User-Agent\"");
        if (UtilMethods.isSet(userAGent)) {
            userEventPayload.put(USER_AGENT_ATTRIBUTE_NAME, userAGent);
        }

        if (!userEventPayload.containsKey(URL_ATTRIBUTE_NAME)) {
            userEventPayload.put(URL_ATTRIBUTE_NAME, "");
        }
    }


    /**
     * Validates events in the provided JSONArray and removes invalid events from the payload list.
     *
     * @param payloadEvents The list of event payloads that will be modified by removing invalid events
     * @param eventsError
     */
    private static void removeUnValidPayloads(
            final List<Map<String, Object>> payloadEvents,
            final List<AnalyticsValidatorUtil.Error> eventsError) {

        if (!eventsError.isEmpty() && payloadEvents != null) {
            final List<Integer> indicesToRemove = eventsError.stream()
                    .map(AnalyticsValidatorUtil.Error::getEventIndex)
                    .distinct()
                    .sorted((a, b) -> Integer.compare(b, a))
                    .collect(Collectors.toList());

            for (int index : indicesToRemove) {
                if (index >= 0 && index < payloadEvents.size()) {
                    payloadEvents.remove(index);
                }
            }
        }
    }

    /**
     * Submits analytics events to the event logging system.
     * 
     * @param request The HTTP servlet request containing host information
     * @param userEventPayload The map containing event payload data to be logged
     * @param eventsErrors The list of validation errors that occurred during event validation
     * @throws RuntimeException If there is an error retrieving the host or logging the event
     */
    private static void sendEvents(
            final HttpServletRequest request,
            final Map<?, ?> userEventPayload,
            final List<AnalyticsValidatorUtil.Error> eventsErrors) {
        try {
            final Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
            SUBMITTER.logEvent(host, new ValidAnalyticsEventPayload((Map<String, Object>) userEventPayload));
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
