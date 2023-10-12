package com.dotcms.jitsu;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrl.Method;
import com.dotcms.http.CircuitBreakerUrl.Response;
import com.dotcms.http.CircuitBreakerUrlBuilder;
import com.dotcms.jitsu.EventsPayload.EventPayload;
import com.dotmarketing.beans.Host;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Optional;

/**
 * POSTs events to established endpoint in EVENT_LOG_POSTING_URL config property using the token set in
 * EVENT_LOG_TOKEN config property
 */
public class EventLogRunnable implements Runnable {

    public static final Map<String, String> POSTING_HEADERS = ImmutableMap.of(
        HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

    private final AnalyticsApp analyticsApp;
    private final EventsPayload eventPayload;

    @VisibleForTesting
    public EventLogRunnable(final Host host) {
        analyticsApp = AnalyticsHelper.get().appFromHost(host);
        eventPayload = null;
    }

    public EventLogRunnable(final Host host, final EventsPayload eventPayload) {
        analyticsApp = AnalyticsHelper.get().appFromHost(host);

        if (StringUtils.isBlank(analyticsApp.getAnalyticsProperties().analyticsWriteUrl())) {
            throw new IllegalStateException("Event log URL is missing, cannot log event to an unknown URL");
        }

        if (StringUtils.isBlank(analyticsApp.getAnalyticsProperties().analyticsKey())) {
            throw new IllegalStateException(
                "Analytics key is missing, cannot log event without a key to identify data with");
        }

        this.eventPayload = eventPayload;
    }

    @Override
    public void run() {

        final String url = analyticsApp.getAnalyticsProperties().analyticsWriteUrl();

        final CircuitBreakerUrlBuilder builder = getCircuitBreakerUrlBuilder(url);

        for (EventPayload payload : eventPayload.payloads()) {

            sendEvent(builder, payload).ifPresent(response -> {
                if (response.getStatusCode() != HttpStatus.SC_OK) {
                    Logger.warn(
                            this.getClass(),
                            String.format(
                                    "Failed to post event to %s, got a %d : %s",
                                    url,
                                    response.getStatusCode(),
                                    response.getResponse()));
                    Logger.warn(this.getClass(), String.format("Failed log: %s", payload));
                }
            });
        }

    }

    private CircuitBreakerUrlBuilder getCircuitBreakerUrlBuilder(String url) {
        return  CircuitBreakerUrl.builder()
            .setMethod(Method.POST)
            .setUrl(url)
            .setParams(map("token", analyticsApp.getAnalyticsProperties().analyticsKey()))
            .setTimeout(4000)
            .setHeaders(POSTING_HEADERS)
            .setThrowWhenNot2xx(false);
    }

    public Optional<Response> sendEvent(final CircuitBreakerUrlBuilder builder, final EventPayload payload) {
        final CircuitBreakerUrl postLog = builder
                .setRawData(payload.toString())
                .build();

        return Optional.ofNullable(
                        Try.of(postLog::doResponse)
                                .onFailure(e -> Logger.warnAndDebug(EventLogRunnable.class, e.getMessage(), e))
                                .getOrElse(CircuitBreakerUrl.EMPTY_RESPONSE));
    }

    public Optional<Response> sendTestEvent() {
        final String url = analyticsApp.getAnalyticsProperties().analyticsWriteUrl();
        final CircuitBreakerUrlBuilder builder = getCircuitBreakerUrlBuilder(url);

        final Map<String, Object> testObject = Map.of("test", "test");

        return sendEvent(builder,
                new EventPayload(new JSONObject(testObject)));

    }

}