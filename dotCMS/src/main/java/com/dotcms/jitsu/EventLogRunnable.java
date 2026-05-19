package com.dotcms.jitsu;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.queue.AnalyticsDeliveryMode;
import com.dotcms.analytics.queue.AnalyticsEventQueuePublisher;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrl.Method;
import com.dotcms.http.CircuitBreakerUrl.Response;
import com.dotcms.http.CircuitBreakerUrlBuilder;
import com.dotcms.jitsu.EventsPayload.EventPayload;
import com.dotmarketing.beans.Host;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpStatus;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * POSTs events to established endpoint in EVENT_LOG_POSTING_URL config property using the token set in
 * EVENT_LOG_TOKEN config property
 */
public class EventLogRunnable implements Runnable {

    public static final Map<String, String> POSTING_HEADERS = ImmutableMap.of(
        HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

    private static final String DOT_ANALYTICS_TENANT = "DOT_ANALYTICS_TENANT";
    private static final String DOT_ANALYTICS_PROJECT = "DOT_ANALYTICS_PROJECT";

    private final AnalyticsApp analyticsApp;
    private final Supplier<EventsPayload> eventPayload;
    private final Supplier<List<Map<String, Serializable>>> rawPayloadSupplier;

    @VisibleForTesting
    public EventLogRunnable(final Host host) {
        analyticsApp = AnalyticsHelper.get().appFromHost(host);
        eventPayload = null;
        rawPayloadSupplier = null;
    }

    public EventLogRunnable(final Host host, final EventsPayload eventPayload) {
        analyticsApp = AnalyticsHelper.get().appFromHost(host);
        rawPayloadSupplier = null;

        if (StringUtils.isBlank(analyticsApp.getAnalyticsProperties().analyticsWriteUrl())) {
            throw new IllegalStateException("Event log URL is missing, cannot log event to an unknown URL");
        }

        if (StringUtils.isBlank(analyticsApp.getAnalyticsProperties().analyticsKey())) {
            throw new IllegalStateException(
                "Analytics key is missing, cannot log event without a key to identify data with");
        }

        this.eventPayload = ()->eventPayload;
    }

    public EventLogRunnable(final Host site, final Supplier<List<Map<String, Serializable>>> payloadSupplier) {
        analyticsApp = AnalyticsHelper.get().appFromHost(site);
        this.rawPayloadSupplier = payloadSupplier;

        if (AnalyticsDeliveryMode.resolve() == AnalyticsDeliveryMode.SQS) {
            this.eventPayload = null;
            return;
        }

        if (StringUtils.isBlank(analyticsApp.getAnalyticsProperties().analyticsWriteUrl())) {
            throw new IllegalStateException("Event log URL is missing, cannot log event to an unknown URL");
        }

        if (StringUtils.isBlank(analyticsApp.getAnalyticsProperties().analyticsKey())) {
            throw new IllegalStateException(
                    "Analytics key is missing, cannot log event without a key to identify data with");
        }

        this.eventPayload = ()-> convertToEventPayload(payloadSupplier.get());
    }

    /**
     * Returns the generated event payload as an {@link Optional} object.
     *
     * @return {@link Optional} of {@link EventsPayload}.
     */
    public Optional<EventsPayload> getEventPayload() {
        return eventPayload != null
                ? Optional.ofNullable(eventPayload.get())
                : Optional.empty();
    }

    protected EventsPayload convertToEventPayload(final List<Map<String, Serializable>> listStringSerializableMap) {

        return new AnalyticsEventsPayload(listStringSerializableMap);
    }

    @Override
    public void run() {

        if (AnalyticsDeliveryMode.resolve() == AnalyticsDeliveryMode.SQS
                && rawPayloadSupplier != null) {
            runSqs();
            return;
        }

        runHttp();
    }

    private void runSqs() {
        final List<Map<String, Serializable>> payloads = rawPayloadSupplier.get();
        if (payloads == null || payloads.isEmpty()) {
            Logger.debug(EventLogRunnable.class, "No analytics events to publish to SQS");
            return;
        }

        final String tenant = Config.getStringProperty(DOT_ANALYTICS_TENANT, "");
        final String project = Config.getStringProperty(DOT_ANALYTICS_PROJECT, "");

        AnalyticsEventQueuePublisher.publish(payloads, tenant, project);
    }

    private void runHttp() {
        final String url = analyticsApp.getAnalyticsProperties().analyticsWriteUrl();
        final CircuitBreakerUrlBuilder builder = getCircuitBreakerUrlBuilder(url);


        final Collection<EventPayload> payloads = eventPayload.get().payloads();

        Logger.debug(EventLogRunnable.class, "Jitsu Event Payload to be sent: " + payloads);

        if (payloads.isEmpty()){
            Logger.error(EventLogRunnable.class,
                    "It is not possible to send the Analytics Event because it has a empty payload: " + eventPayload.get().payloads());
            return;
        }

        final EventPayload firstPayload = payloads.stream().findFirst().orElse(null);


        if (firstPayload == null) {
            Logger.warn(EventLogRunnable.class, "Not Jitsu Events Payload to be sent");
            return;
        }

        final String userAgent = firstPayload.contains(ValidAnalyticsEventPayloadAttributes.USER_AGENT_ATTRIBUTE_NAME) ?
                firstPayload.get(ValidAnalyticsEventPayloadAttributes.USER_AGENT_ATTRIBUTE_NAME).toString() : null;

        sendEvent(builder, payloads, userAgent).ifPresent(response -> {

            Logger.debug(EventLogRunnable.class, "Jitsu Event Response: " + response.getStatusCode() +
             ", message: " + response.getResponse());

            if (response.getStatusCode() != HttpStatus.SC_OK) {
                Logger.warn(
                        this.getClass(),
                        String.format(
                                "Failed to post event to %s, got a %d : %s",
                                url,
                                response.getStatusCode(),
                                response.getResponse()));

                Logger.warn(this.getClass(), String.format("Failed log: number of events to be send %s", payloads.size()));
            }
        });
    }


    private CircuitBreakerUrlBuilder getCircuitBreakerUrlBuilder(String url) {
        return  CircuitBreakerUrl.builder()
            .setMethod(Method.POST)
            .setUrl(url)
            .setParams(Map.of("token", analyticsApp.getAnalyticsProperties().analyticsKey()))
            .setTimeout(4000)
            .setHeaders(POSTING_HEADERS)
            .setThrowWhenError(false);
    }


    public Optional<Response<String>> sendEvent(final CircuitBreakerUrlBuilder builder,
                                                final Collection<EventPayload> payload,
                                                final String userAgent) {

        final CircuitBreakerUrlBuilder circuitBreakerUrlBuilder = builder.setRawData(payload.toString());

        if (UtilMethods.isSet(userAgent)) {
            circuitBreakerUrlBuilder.setHeaders(Map.of(HttpHeaders.USER_AGENT, userAgent));
        }

        final CircuitBreakerUrl postLog = circuitBreakerUrlBuilder.build();

        return Optional.ofNullable(
                        Try.of(postLog::doResponse)
                                .onFailure(e -> Logger.warnAndDebug(EventLogRunnable.class, e.getMessage(), e))
                                .getOrElse(CircuitBreakerUrl.EMPTY_RESPONSE));
    }

    public Optional<Response<String>> sendTestEvent() {
        final String url = analyticsApp.getAnalyticsProperties().analyticsWriteUrl();
        final CircuitBreakerUrlBuilder builder = getCircuitBreakerUrlBuilder(url);

        final Map<String, Object> testObject = Map.of("test", "test");

        return sendEvent(builder,
                list( new EventPayload(new JSONObject(testObject))), "dotcms/test");

    }
}
