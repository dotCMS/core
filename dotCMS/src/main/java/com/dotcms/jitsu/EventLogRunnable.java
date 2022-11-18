package com.dotcms.jitsu;

import com.dotcms.analytics.AnalyticsAPI;
import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotmarketing.beans.Host;

import java.util.Map;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrl.Method;
import com.dotcms.http.CircuitBreakerUrlBuilder;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

/**
 * POSTs events to established endpoint in EVENT_LOG_POSTING_URL config property using the token set in
 * EVENT_LOG_TOKEN config property
 */
public class EventLogRunnable implements Runnable {

    private static final Map<String, String> POSTING_HEADERS = ImmutableMap.of(
        HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

    private final AnalyticsApp analyticsApp;
    private final AnalyticsAPI analyticsAPI;
    private final String log;

    EventLogRunnable(Host host, final String log) {
        analyticsApp = AnalyticsHelper.getHostApp(host);
        analyticsAPI = APILocator.getAnalyticsAPI();

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
        final String url = analyticsApp + "?token=" + analyticsApp.getAnalyticsProperties().analyticsKey();
        final CircuitBreakerUrlBuilder builder;
        try {
            builder = CircuitBreakerUrl.builder()
                .setMethod(Method.POST)
                .setUrl(url)
                .setTimeout(4000)
                .setHeaders(prepareHeaders());
        } catch (DotDataException e) {
            Logger.error(this, String.format("Could not resolve access token when posting events to %s", url), e);
            return;
        }

        final CircuitBreakerUrl postLog = builder.setRawData(log).build();

        int response = 0;
        String responseString = null;
        try {
            responseString = postLog.doString();
            response = postLog.response();
        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), e.getMessage(), e);
        }

        if (response != HttpStatus.SC_OK) {
            Logger.warn(
                this.getClass(),
                String.format("Failed to post event to %s, got a %d : %s", url, response, responseString));
            Logger.warn(this.getClass(), String.format("Failed log: " + log));
        }
    }

    private Map<String, String> prepareHeaders() throws DotDataException {
        return ImmutableMap.<String, String>builder()
            .putAll(POSTING_HEADERS)
            .put(HttpHeaders.AUTHORIZATION, analyticsAPI.fetchAccessToken(analyticsApp).accessToken())
            .build();
    }

}