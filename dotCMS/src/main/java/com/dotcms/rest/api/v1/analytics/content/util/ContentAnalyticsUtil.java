package com.dotcms.rest.api.v1.analytics.content.util;

import com.dotcms.analytics.track.collectors.Collector;
import com.dotcms.analytics.track.collectors.EventSource;
import com.dotcms.analytics.track.collectors.EventType;
import com.dotcms.jitsu.EventLogSubmitter;
import com.dotcms.jitsu.ValidAnalyticsEventPayload;
import com.dotcms.jitsu.ValidAnalyticsEventPayloadAttributes;
import com.dotcms.jitsu.validators.AnalyticsValidatorUtil;
import com.dotcms.rest.api.v1.analytics.event.UrlParamFilter;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import io.vavr.Lazy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.dotcms.jitsu.ValidAnalyticsEventPayloadAttributes.REFERER_ATTRIBUTE_NAME;
import static com.dotcms.jitsu.ValidAnalyticsEventPayloadAttributes.URL_ATTRIBUTE_NAME;
import static com.dotcms.jitsu.ValidAnalyticsEventPayloadAttributes.USER_AGENT_ATTRIBUTE_NAME;

/**
 * This class exposes useful methods for generating Content Analytics events and interacting with
 * its configuration parameters.
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
     * App-secret key under which the comma-separated list of extra query parameter names to keep
     * in page URLs is stored. See {@link com.dotcms.rest.api.v1.analytics.event.UrlParamFilter}.
     */
    public static final String ALLOWED_URL_PARAMS_KEY = "allowedUrlParams";

    /**
     * App-secret key under which the site's choice to filter query parameters out of page URLs is
     * stored. Filtering applies unless this is explicitly {@code false}. See
     * {@link com.dotcms.rest.api.v1.analytics.event.UrlParamFilter}.
     */
    public static final String FILTER_URL_PARAMS_KEY = "filterUrlParams";

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
            filterEventUrls(request, userEventPayload);
        }

        if (!payloadEvents.isEmpty()) {
            includeAutomaticallyFields(userEventPayload, request);
            sendEvents(request, userEventPayload, eventsErrors);
        }

        return new AnalyticsEventsResult.Builder()
                .addEventsErrors(eventsErrors)
                .addTotalEvents(totalEvents)
                .build();
    }

    /**
     * Cleans the page URL fields of the events before they are sent: {@code page.url},
     * {@code page.doc_search}, {@code page.doc_hash} and a clicked link's {@code href} keep only the
     * allowlisted query parameters, and user info and session ids are removed (see
     * {@link UrlParamFilter}). The site can turn the parameter filter off with its
     * {@code filterUrlParams} App field, in which case only user info and session ids are removed.
     * Events whose {@code data} or {@code page} is not an object are removed from the payload.
     *
     * @param request          the current request, used to resolve the site
     * @param userEventPayload the event payload; its events are cleaned in place
     */
    private static void filterEventUrls(final HttpServletRequest request,
                                        final Map<String, Serializable> userEventPayload) {
        try {
            final Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
            applyUrlParamFilter((Map<String, Object>) (Map<?, ?>) userEventPayload, getAppSecrets(host));
        } catch (DotDataException | DotSecurityException | PortalException | SystemException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Applies the URL filter to an event payload, in place, according to the site's App secrets.
     *
     * @param payload    the event payload ({@code events[].data.page...})
     * @param siteSecrets the site's Content Analytics App secrets; may be empty
     */
    static void applyUrlParamFilter(final Map<String, Object> payload,
                                    final Map<String, Secret> siteSecrets) {
        if (UrlParamFilter.isEnabled(siteSecrets)) {
            UrlParamFilter.filterEventPages(payload, UrlParamFilter.resolveAllowedParams(siteSecrets));
        } else {
            UrlParamFilter.removeUserInfoAndSessionIds(payload);
        }
    }

    /**
     * Retrieves all app secrets for the Content Analytics app for the given site.
     *
     * @param currentSite The site to retrieve the secrets for
     * @return Map of secret keys to Secret objects, or an empty map if not found or unreadable
     */
    public static Map<String, Secret> getAppSecrets(final Host currentSite) {
        try {
            return APILocator.getAppsAPI()
                    .getSecrets(CONTENT_ANALYTICS_APP_KEY, true, currentSite, APILocator.systemUser())
                    .map(AppSecrets::getSecrets)
                    .orElse(Collections.emptyMap());
        } catch (final DotDataException | DotSecurityException e) {
            Logger.error(ContentAnalyticsUtil.class,
                    "Error retrieving app secrets for site: " + currentSite.getIdentifier(), e);
            return Collections.emptyMap();
        }
    }

    private static void includeAutomaticallyFields(final Map<String, Serializable> userEventPayload,
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

    /**
     * Generates the encrypted site key that will be used by JavaScript code in HTML Pages to send
     * Analytics Events to our infrastructure. This allows us to provide customers with a secure
     * token that must be passed down to our REST Endpoint in order to varify that the request is
     * coming from a valid source.
     *
     * @param siteId The Identifier of the Site that the Content Analytics configuration belongs
     *               to.
     *
     * @return The Encrypted Site Key
     */
    public static String generateInternalSiteKey(final String siteId) {
        return String.format(SITE_KEY_FORMAT.get(), siteId, KeyGenerator.generateSiteKey());
    }

}
