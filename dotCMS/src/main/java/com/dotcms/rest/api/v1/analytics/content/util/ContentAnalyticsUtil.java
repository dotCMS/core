package com.dotcms.rest.api.v1.analytics.content.util;

import com.dotcms.analytics.track.collectors.Collector;
import com.dotcms.analytics.track.collectors.EventSource;
import com.dotcms.analytics.track.collectors.EventType;
import com.dotcms.jitsu.EventLogSubmitter;
import com.dotcms.jitsu.ValidAnalyticsEventPayload;
import com.dotcms.jitsu.ValidAnalyticsEventPayloadAttributes;
import com.dotcms.jitsu.validators.AnalyticsValidatorUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import io.vavr.Lazy;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.dotcms.jitsu.ValidAnalyticsEventPayloadAttributes.REFERER_ATTRIBUTE_NAME;
import static com.dotcms.jitsu.ValidAnalyticsEventPayloadAttributes.URL_ATTRIBUTE_NAME;
import static com.dotcms.jitsu.ValidAnalyticsEventPayloadAttributes.USER_AGENT_ATTRIBUTE_NAME;

/**
 * This utility class provides different methods for interacting with Content Analytics features.
 *
 * @author Jonathan Sanchez
 * @since Mar 12th, 2025
 */
public class ContentAnalyticsUtil {

    private static final Lazy<String> SITE_KEY_FORMAT = Lazy.of(() -> Config.getStringProperty("CONTENT_ANALYTICS_SITE_KEY_FORMAT", "DOT.%s.%s"));

    private static final AnalyticsValidatorUtil analyticsValidatorUtil =  AnalyticsValidatorUtil.INSTANCE;

    private static final EventLogSubmitter SUBMITTER  = new EventLogSubmitter();

    public static final String CONTENT_ANALYTICS_APP_KEY = "dotContentAnalytics-config";

    /**
     * Persists a user-defined event to the Content Analytics system. Several validation criteria
     * can be applied to the event payload before it is persisted. The
     * {@link com.dotcms.jitsu.validators.AnalyticsValidatorProcessor} allows developers to create
     * both global validators, and event-specific validators.
     *
     * @param request          The current instance of the {@link HttpServletRequest}.
     * @param userEventPayload The map containing event payload data.
     *
     * @return The {@link AnalyticsEventsResult} containing the result of the operation.
     */
    @SuppressWarnings("unchecked")
    public static AnalyticsEventsResult registerContentAnalyticsRestEvent(
            final HttpServletRequest request,
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
        removeInvalidEventsFromPayload(payloadEvents, eventsErrors);

        if (!payloadEvents.isEmpty()) {
            includeInternalFields(userEventPayload, request);
            sendEvents(request, userEventPayload);
        }

        return new AnalyticsEventsResult.Builder()
                .addEventsErrors(eventsErrors)
                .addTotalEvents(totalEvents)
                .build();
    }

    /**
     * Includes several internal dotCMS-specific attributes to the JSON Payload. These type of
     * attributes are either not exposed to the customer, or must be handled internally for
     * consistency reasons.
     *
     * @param userEventPayload The payload containing the attributes sent by the client.
     * @param request          The current instance of the {@link HttpServletRequest}.
     */
    private static void includeInternalFields(final Map<String, Serializable> userEventPayload,
                                                   final HttpServletRequest request) {

        final String requestId = request.getAttribute("requestId").toString();
        userEventPayload.put("request_id", requestId);

        final String referer = request.getHeader("Referer");
        if (UtilMethods.isSet(referer)) {
            userEventPayload.put(REFERER_ATTRIBUTE_NAME, referer);
        }

        final String userAGent = request.getHeader("User-Agent");
        if (UtilMethods.isSet(userAGent)) {
            userEventPayload.put(USER_AGENT_ATTRIBUTE_NAME, userAGent);
        }

        if (!userEventPayload.containsKey(URL_ATTRIBUTE_NAME)) {
            userEventPayload.put(URL_ATTRIBUTE_NAME, "");
        }

        userEventPayload.put("isExperimentPage", false);
        userEventPayload.put("isTargetPage", false);
    }

    /**
     * Validates the events in the provided JSONArray and removes invalid events from the payload
     * list.
     *
     * @param payloadEvents The list of event payloads that will be modified by removing invalid
     *                      events
     * @param eventsError   The list of validation errors that occurred during event validation.
     */
    private static void removeInvalidEventsFromPayload(
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
     * @throws RuntimeException If there is an error retrieving the host or logging the event
     */
    @SuppressWarnings("unchecked")
    private static void sendEvents(
            final HttpServletRequest request,
            final Map<?, ?> userEventPayload) {
        try {
            final Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
            SUBMITTER.logEvent(host, new ValidAnalyticsEventPayload((Map<String, Object>) userEventPayload));
        } catch (DotDataException | DotSecurityException | PortalException | SystemException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates the encrypted site key that will be used by JavaScript code in HTML Pages to send
     * Analytics Events to our infrastructure. This allows us to provide customers with a secure
     * token that must be passed down to our REST Endpoint in order to varify that the request is
     * coming from a valid source.
     * <p>The {@code DOT_CONTENT_ANALYTICS_SITE_KEY_FORMAT} environment variable allows you to
     * customize the format of the generated site key, if required.By default, it requires both the
     * Site ID, and the generated random Site Key.</p>
     *
     * @param siteId The Identifier of the Site that the Content Analytics configuration belongs
     *               to.
     *
     * @return The secure random Site Key.
     */
    public static String generateInternalSiteKey(final String siteId) {
        return String.format(SITE_KEY_FORMAT.get(), siteId, KeyGenerator.generateSiteKey());
    }

}
