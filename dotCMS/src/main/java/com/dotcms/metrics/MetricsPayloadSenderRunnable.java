package com.dotcms.metrics;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.model.AnalyticsProperties;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrl.Response;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.control.Try;

import java.util.Optional;

/**
 * Base class for sending metrics to the analytics endpoint.
 *
 * @author vico
 */
public class MetricsPayloadSenderRunnable extends BaseMetricsSenderRunnable<String> {

    public MetricsPayloadSenderRunnable(final AnalyticsApp analyticsApp, final String payload) {
        super(analyticsApp, payload);
    }

    @VisibleForTesting
    public MetricsPayloadSenderRunnable(final AnalyticsApp analyticsApp) {
        super(analyticsApp);
    }

    @Override
    public void run() {
        sendEvent(getCircuitBreakerUrl(payload)).ifPresent(this::logResponse);
    }

    private Optional<Response<String>> sendEvent(final CircuitBreakerUrl circuitBreakerUrl) {
        final AnalyticsProperties analyticsProperties = analyticsApp.getAnalyticsProperties();
        Logger.debug(
                getClass(),
                String.format(
                        "Sending to [%s] the following metrics: %s",
                        analyticsProperties.analyticsWriteUrl(),
                        payload));
        final CircuitBreakerUrl.Response<String> response = Try
                .of(circuitBreakerUrl::doResponse)
                .onFailure(e -> Logger.warn(this, e.getMessage(), e))
                .getOrElse(CircuitBreakerUrl.EMPTY_RESPONSE);
        Logger.debug(
                this,
                String.format(
                        "Response from metrics endpoint [%s]: %s",
                        analyticsProperties.analyticsWriteUrl(),
                        response));
        return Optional.of(response);
    }

}