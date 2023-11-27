package com.dotcms.jitsu;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrl.Response;
import com.dotcms.http.CircuitBreakerUrlBuilder;
import com.dotcms.jitsu.EventsPayload.EventPayload;
import com.dotcms.metrics.BaseMetricsSenderRunnable;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.control.Try;

import java.util.Map;
import java.util.Optional;

/**
 * POSTs events to established endpoint in EVENT_LOG_POSTING_URL config property using the token set in
 * EVENT_LOG_TOKEN config property
 */
public class EventLogRunnable extends BaseMetricsSenderRunnable<EventsPayload>  {

    public EventLogRunnable(final AnalyticsApp analyticsApp, final EventsPayload payload) {
        super(analyticsApp, payload);
    }

    @VisibleForTesting
    public EventLogRunnable(final AnalyticsApp analyticsApp) {
        super(analyticsApp);
    }

    @Override
    public void run() {
        final String url = analyticsApp.getAnalyticsProperties().analyticsWriteUrl();
        final CircuitBreakerUrlBuilder builder = getCircuitBreakerUrlBuilder(url);

        for (EventPayload payload : payload.payloads()) {
            sendEvent(builder, payload).ifPresent(this::logResponse);
        }
    }

    private Optional<Response<String>> sendEvent(final CircuitBreakerUrlBuilder builder, final EventPayload payload) {
        final CircuitBreakerUrl postLog = builder
                .setRawData(payload.toString())
                .build();

        return Optional.ofNullable(
                Try.of(postLog::doResponse)
                        .onFailure(e -> Logger.warnAndDebug(EventLogRunnable.class, e.getMessage(), e))
                        .getOrElse(CircuitBreakerUrl.EMPTY_RESPONSE));
    }

    public Optional<Response<String>> sendTestEvent() {
        final String url = analyticsApp.getAnalyticsProperties().analyticsWriteUrl();
        final CircuitBreakerUrlBuilder builder = getCircuitBreakerUrlBuilder(url);
        final Map<String, Object> testObject = Map.of("test", "test");
        return sendEvent(builder, new EventPayload(new JSONObject(testObject)));
    }

}