package com.dotcms.jitsu;

import com.dotmarketing.util.Config;
import java.util.Map;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrl.Method;
import com.dotcms.http.CircuitBreakerUrlBuilder;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;

/**
 * POSTs events to established endpoint in EVENT_LOG_POSTING_URL config property using the token set in
 * EVENT_LOG_TOKEN config property
 */

public class EventLogRunnable implements Runnable {

    final String POST_URL = Config.getStringProperty("EVENT_LOG_POSTING_URL");

    final Map<String, String> POSTING_HEADERS = ImmutableMap.of("content-type", "application/json");

    final String token = Config.getStringProperty("EVENT_LOG_TOKEN");
    final String log;

    EventLogRunnable(final String log) {
        this.log = log;

    }

    private final CircuitBreakerUrlBuilder builder = CircuitBreakerUrl.builder()
            .setMethod(Method.POST)
            .setUrl(POST_URL + "?token=" + this.token)
            .setTimeout(4000)
            .setHeaders(POSTING_HEADERS);

    @Override
    public void run() {
        CircuitBreakerUrl postLog = builder.setRawData(log).build();

        int response = 0;
        String responseString = null;
        try {
            responseString=postLog.doString();
            response = postLog.response();

        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), e.getMessage(), e);
        }
        if(response!=200) {
            Logger.warn(this.getClass(), "failed to post event, got a " + response + " : " + responseString  );
            Logger.warn(this.getClass(), "failed log: " + log  );

        }

    }

}