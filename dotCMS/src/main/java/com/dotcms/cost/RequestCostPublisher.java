package com.dotcms.cost;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;

/**
 * Ships {@link RequestCostSnapshot} payloads to an external REST endpoint on each tick of
 * {@link RequestCostApiImpl#logRequestCost()}.
 *
 * <p>Activates implicitly when both {@code REQUEST_COST_PUSH_URL} and
 * {@code REQUEST_COST_PUSH_TOKEN} are set. Failures are rate-limited warnings and the snapshot
 * is dropped — this is observational telemetry, not durable accounting.</p>
 *
 * <p>Config keys:
 * <ul>
 *   <li>{@code REQUEST_COST_PUSH_URL} (presence with token activates the publisher)</li>
 *   <li>{@code REQUEST_COST_PUSH_TOKEN} (bearer token; presence with url activates the publisher)</li>
 *   <li>{@code REQUEST_COST_PUSH_TIMEOUT_MS} (default 5000)</li>
 * </ul>
 * </p>
 */
@ApplicationScoped
public class RequestCostPublisher {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long FAIL_LOG_INTERVAL_MS = 60 * 10 * 1000L;

    public boolean isEnabled() {
        return UtilMethods.isSet(getUrl()) && UtilMethods.isSet(getToken());
    }

    private String getUrl() {
        return Config.getStringProperty("REQUEST_COST_PUSH_URL", null);
    }

    private String getToken() {
        return Config.getStringProperty("REQUEST_COST_PUSH_TOKEN", null);
    }

    private long getTimeoutMs() {
        return Config.getLongProperty("REQUEST_COST_PUSH_TIMEOUT_MS", 5_000L);
    }

    /**
     * Submits the snapshot to {@link DotConcurrentFactory}'s default submitter so the HTTP POST
     * never blocks the request-cost monitor scheduler. Returns immediately. Transport errors are
     * logged at most once per minute and the snapshot is dropped.
     */
    public void publish(final RequestCostSnapshot snapshot) {
        if (!isEnabled()) {
            return;
        }
        DotConcurrentFactory.getInstance().getSubmitter().submit(() -> post(snapshot));
    }

    private void post(final RequestCostSnapshot snapshot) {
        final String url = getUrl();
        if (!UtilMethods.isSet(url)) {
            return;
        }
        try {
            final Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            final String token = getToken();
            if (UtilMethods.isSet(token)) {
                headers.put("Authorization", "Bearer " + token);
            }

            final CircuitBreakerUrl call = CircuitBreakerUrl.builder()
                    .setMethod(CircuitBreakerUrl.Method.POST)
                    .setUrl(url)
                    .setHeaders(headers)
                    .setRawData(MAPPER.writeValueAsString(snapshot))
                    .setTimeout(getTimeoutMs())
                    .setThrowWhenError(false)
                    .build();

            call.doString();
            final int response = call.response();
            if (!CircuitBreakerUrl.isSuccessResponse(response)) {
                Logger.warnEvery(this.getClass(),
                        "REQUEST_COST_PUSH_FAIL",
                        "Request cost push to " + url + " returned HTTP " + response,
                        (int) FAIL_LOG_INTERVAL_MS);
            }
        } catch (final Exception e) {
            Logger.warnEvery(this.getClass(),
                    "REQUEST_COST_PUSH_ERR",
                    "Request cost push to " + url + " failed: " + e.getMessage(),
                    (int) FAIL_LOG_INTERVAL_MS);
        }
    }
}
