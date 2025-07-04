package com.dotcms.rest.api.v1.analytics.content.util;

import com.dotcms.analytics.track.collectors.Collector;
import com.dotcms.analytics.track.collectors.EventSource;
import com.dotcms.analytics.track.collectors.EventType;
import com.dotcms.jitsu.EventLogSubmitter;
import com.dotcms.jitsu.ValidAnalyticsEventPayload;
import com.dotcms.jitsu.ValidAnalyticsEventPayloadAttributes;
import com.dotcms.jitsu.validators.AnalyticsValidatorUtil;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
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
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dotcms.jitsu.ValidAnalyticsEventPayloadAttributes.REFERER_ATTRIBUTE_NAME;
import static com.dotcms.jitsu.ValidAnalyticsEventPayloadAttributes.URL_ATTRIBUTE_NAME;
import static com.dotcms.jitsu.ValidAnalyticsEventPayloadAttributes.USER_AGENT_ATTRIBUTE_NAME;

/**
 *
 * @author Jonathan Sanchez
 * @since Mar 12th, 2025
 */
public class ContentAnalyticsUtil {

    private static final Lazy<String> SITE_KEY_FORMAT = Lazy.of(() -> Config.getStringProperty("CONTENT_ANALYTICS_SITE_KEY_FORMAT", "DOT.%s.%s"));
    private static final String SAMPLE_CA_JS_CONFIG = "const analyticsConfig = {\n" +
                "\tsiteKey: '%s',\n" +
                "\tserver: '%s'\n" +
            "}";

    private static final AnalyticsValidatorUtil analyticsValidatorUtil =  AnalyticsValidatorUtil.INSTANCE;

    private static final EventLogSubmitter SUBMITTER  = new EventLogSubmitter();

    public static final String CONTENT_ANALYTICS_APP_KEY = "dotContentAnalytics-config";

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
     * Exposes a sample basic JavaScript configuration object that customers can copy and paste to
     * configure their code to send Analytics Events to our infrastructure.
     *
     * @param site The {@link Host} that the configuration belongs to.
     *
     * @return The sample JavaScript configuration.
     */
    public static String getSiteJSConfig(final Host site) throws DotDataException, DotSecurityException {
        final String siteKey = getSiteKey(site);
        return String.format(SAMPLE_CA_JS_CONFIG, siteKey, "https://" + site.getHostname());
    }

    /**
     * Returns the authentication key for a specific Site. If the user has NOT provided a custom
     * key, then dotCMS will generate one for them.
     *
     * @param site The {@link Host} that the configuration belongs to.
     *
     * @return The Site Key.
     *
     * @throws DotDataException     An error occurred when updating the App's secrets.
     * @throws DotSecurityException A permission error occurred when reading/saving App data.
     */
    public static String getSiteKey(final Host site) throws DotDataException, DotSecurityException {
        final AppsAPI appsAPI = APILocator.getAppsAPI();
        final Optional<AppSecrets> optionalAppSecrets = appsAPI.getSecrets(CONTENT_ANALYTICS_APP_KEY, false, site, APILocator.systemUser());
        if (optionalAppSecrets.isPresent()) {
            final Set<Map.Entry<String, Secret>> appParams = optionalAppSecrets.get().getSecrets().entrySet();
            final Optional<String> optSiteKey = appParams.stream()
                    .filter(entry -> "siteKey".equals(entry.getKey()))
                    .map(entry -> entry.getValue().getString())
                    .findFirst();
            if (optSiteKey.isPresent() && !optSiteKey.get().isEmpty()) {
                return optSiteKey.get();
            }
        }
        // The App config or Site Key doesn't exist, let's create it
        final String siteKey = generateInternalSiteKey(site.getIdentifier());
        final AppSecrets.Builder builder = new AppSecrets.Builder();
        builder.withKey(CONTENT_ANALYTICS_APP_KEY);
        optionalAppSecrets
                .map(appSecrets -> appSecrets.getSecrets().entrySet())
                .orElse(Set.of())
                .forEach(entry -> builder.withSecret(entry.getKey(), entry.getValue()));
        builder.withSecret("siteKey", siteKey);
        final AppSecrets secrets = builder.build();
        appsAPI.saveSecrets(secrets, site, APILocator.systemUser());
        return siteKey;
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
    private static String generateInternalSiteKey(final String siteId) {
        return String.format(SITE_KEY_FORMAT.get(), siteId, KeyGenerator.generateSiteKey());
    }

}
