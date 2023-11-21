package com.dotcms.metrics;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrlBuilder;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.Serializable;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;

/**
 * Base class for sending metrics to the analytics endpoint
 * @param <P>
 *
 * @author vico
 */
public abstract class BaseMetricsSenderRunnable<P extends Serializable> implements Runnable {

    public static final String TOKEN_QUERY_PARAM_NAME = "token";
    public static final Map<String, String> METRICS_SENDER_HEADERS = ImmutableMap.of(
            HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

    protected final AnalyticsApp analyticsApp;
    protected final P payload;

    public BaseMetricsSenderRunnable(final AnalyticsApp analyticsApp, final P payload) {
        this.analyticsApp = analyticsApp;
        this.payload = payload;
        validateApp();
    }

    @VisibleForTesting
    public BaseMetricsSenderRunnable(final AnalyticsApp analyticsApp) {
        this(analyticsApp, null);
    }

    private void validateApp() {
        if (StringUtils.isBlank(analyticsApp.getAnalyticsProperties().analyticsWriteUrl())) {
            throw new IllegalStateException("Event log URL is missing, cannot log event to an unknown URL");
        }

        if (StringUtils.isBlank(analyticsApp.getAnalyticsProperties().analyticsKey())) {
            throw new IllegalStateException(
                    "Analytics key is missing, cannot log event without a key to identify data with");
        }
    }

    protected CircuitBreakerUrlBuilder getCircuitBreakerUrlBuilder(final String url) {
        return CircuitBreakerUrl.builder()
                .setMethod(CircuitBreakerUrl.Method.POST)
                .setUrl(url)
                .setParams(map(TOKEN_QUERY_PARAM_NAME, analyticsApp.getAnalyticsProperties().analyticsKey()))
                .setTimeout(4000)
                .setHeaders(METRICS_SENDER_HEADERS)
                .setThrowWhenNot2xx(false);
    }

    protected CircuitBreakerUrlBuilder getCircuitBreakerUrlBuilder() {
        return getCircuitBreakerUrlBuilder(analyticsApp.getAnalyticsProperties().analyticsWriteUrl());
    }

    protected CircuitBreakerUrl getCircuitBreakerUrl(final P payload) {
        return getCircuitBreakerUrlBuilder().setRawData(payload.toString()).build();
    }

    protected void logResponse(final CircuitBreakerUrl.Response<String> response) {
        if (response.getStatusCode() != HttpStatus.SC_OK) {
            Logger.warn(
                    this,
                    String.format(
                            "Failed to send metric to %s, got a %d : %s",
                            analyticsApp.getAnalyticsProperties().analyticsWriteUrl(),
                            response.getStatusCode(),
                            response.getResponse()));
            Logger.warn(this, String.format("Failed metrics post: %s", payload));
        }
    }

}