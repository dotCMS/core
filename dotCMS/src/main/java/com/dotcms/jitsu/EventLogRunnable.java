package com.dotcms.jitsu;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrl.Method;
import com.dotmarketing.beans.Host;
import com.dotmarketing.util.Logger;
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

    private static final Map<String, String> POSTING_HEADERS = ImmutableMap.of(
        HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

    private final AnalyticsApp analyticsApp;
    private final String log;

    EventLogRunnable(final Host host, final String log) {
        analyticsApp = AnalyticsHelper.appFromHost(host);

        if (StringUtils.isBlank(analyticsApp.getAnalyticsProperties().analyticsWriteUrl())) {
            throw new IllegalStateException("Event log URL is missing, cannot log event to an unknown URL");
        }

        if (StringUtils.isBlank(analyticsApp.getAnalyticsProperties().analyticsKey())) {
            throw new IllegalStateException(
                "Analytics key is missing, cannot log event without a key to identify data with");
        }

        this.log = log;
    }

    @Override
    public void run() {
        final String url = analyticsApp.getAnalyticsProperties().analyticsWriteUrl()
            + "?token="
            + analyticsApp.getAnalyticsProperties().analyticsKey();

        final CircuitBreakerUrl postLog = CircuitBreakerUrl.builder()
            .setMethod(Method.GET)
            .setUrl(url)
            .setTimeout(4000)
            .setHeaders(POSTING_HEADERS)
            .setRawData(log)
            .build();

        Optional.ofNullable(
            Try.of(postLog::doResponse)
                .onFailure(e -> Logger.warnAndDebug(EventLogRunnable.class, e.getMessage(), e))
                .getOrElse(CircuitBreakerUrl.EMPTY_RESPONSE))
            .ifPresent(response -> {
                if (response.getStatusCode() != HttpStatus.SC_OK) {
                    Logger.warn(
                        this.getClass(),
                        String.format(
                            "Failed to post event to %s, got a %d : %s",
                            url,
                            response.getStatusCode(),
                            response.getResponse()));
                    Logger.warn(this.getClass(), String.format("Failed log: " + log));
                }
            });
    }

}