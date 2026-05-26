package com.dotcms.cost;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private static final int FAIL_LOG_INTERVAL_MS = 10 * 60 * 1000;
    private final AtomicBoolean httpSchemeWarned = new AtomicBoolean(false);

    public boolean isEnabled() {
        // Use the sanitized token in the gate so a whitespace-or-CRLF-only token
        // doesn't activate the publisher and cause an unauthenticated POST.
        return UtilMethods.isSet(getUrl()) && UtilMethods.isSet(sanitizeHeaderValue(getToken()));
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
     * logged at most once every 10 minutes and the snapshot is dropped.
     */
    public void publish(final RequestCostSnapshot snapshot) {
        if (!isEnabled()) {
            return;
        }
        DotConcurrentFactory.getInstance().getSubmitter().submit(() -> post(snapshot));
    }

    private void post(final RequestCostSnapshot snapshot) {
        final String url = getUrl();
        final String token = sanitizeHeaderValue(getToken());
        // Re-check both pieces — config may have been cleared between submit and execute, and
        // posting without an Authorization header is a worse failure mode than not posting at all.
        if (!UtilMethods.isSet(url) || !UtilMethods.isSet(token)) {
            return;
        }
        warnOncePlainHttp(url);
        try {
            // CircuitBreakerUrl sniffs the rawData and applies Content-Type automatically when
            // the payload starts with '{', so we don't set it explicitly (would risk a duplicate
            // header on some Apache HttpClient versions).
            final Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + token);

            final CircuitBreakerUrl call = CircuitBreakerUrl.builder()
                    .setMethod(CircuitBreakerUrl.Method.POST)
                    .setUrl(url)
                    .setHeaders(headers)
                    .setRawData(MAPPER.writeValueAsString(snapshot))
                    .setTimeout(getTimeoutMs())
                    .setThrowWhenError(false)
                    .build();

            call.doString();
            if (!call.isProcessed()) {
                Logger.warnEvery(this.getClass(),
                        "REQUEST_COST_PUSH_FAIL_TRANSPORT",
                        "Request cost push to " + sanitizeUrlForLog(url) + " did not complete (circuit open or transport error)",
                        FAIL_LOG_INTERVAL_MS);
                return;
            }
            final int response = call.response();
            if (!CircuitBreakerUrl.isSuccessResponse(response)) {
                Logger.warnEvery(this.getClass(),
                        "REQUEST_COST_PUSH_FAIL_HTTP",
                        "Request cost push to " + sanitizeUrlForLog(url) + " returned HTTP " + response,
                        FAIL_LOG_INTERVAL_MS);
            }
        } catch (final Exception e) {
            Logger.warnEvery(this.getClass(),
                    "REQUEST_COST_PUSH_ERR_EXCEPTION",
                    "Request cost push to " + sanitizeUrlForLog(url) + " failed: " + e.getMessage(),
                    FAIL_LOG_INTERVAL_MS);
            Logger.debug(this.getClass(),
                    "Request cost push to " + sanitizeUrlForLog(url) + " failed with exception",
                    e);
        }
    }

    /**
     * Warns once (per process lifetime) if the configured URL uses the plain {@code http://}
     * scheme — the bearer token would otherwise traverse the wire in cleartext. Stays a warning
     * rather than a refusal so a misconfiguration doesn't silently drop telemetry.
     */
    private void warnOncePlainHttp(final String url) {
        if (httpSchemeWarned.get()) {
            return;
        }
        try {
            final String scheme = URI.create(url).getScheme();
            if (scheme != null && scheme.equalsIgnoreCase("http")
                    && httpSchemeWarned.compareAndSet(false, true)) {
                Logger.warn(this.getClass(),
                        "REQUEST_COST_PUSH_URL uses plain http:// — bearer token will be sent "
                                + "in cleartext. Use https:// for any non-loopback destination.");
            }
        } catch (final Exception ignored) {
            // sanitizer is best-effort
        }
    }

    /**
     * Strips CR/LF and surrounding whitespace from a header value. A misconfigured
     * {@code REQUEST_COST_PUSH_TOKEN} containing CRLF would otherwise enable HTTP header
     * injection — low-risk since only operators set this, but cheap to harden.
     */
    static String sanitizeHeaderValue(final String value) {
        return value == null ? null : value.replace("\r", "").replace("\n", "").trim();
    }

    /**
     * Strips RFC-3986 userinfo (the {@code user:pass@} segment) from a URL before logging so a
     * misconfigured {@code REQUEST_COST_PUSH_URL=https://user:secret@host/...} doesn't leak the
     * credential into every failure log line.
     */
    static String sanitizeUrlForLog(final String url) {
        if (!UtilMethods.isSet(url)) {
            return url;
        }
        try {
            final URI uri = URI.create(url);
            if (uri.getUserInfo() == null) {
                return url;
            }
            final URI safe = new URI(
                    uri.getScheme(), null, uri.getHost(), uri.getPort(),
                    uri.getPath(), uri.getQuery(), uri.getFragment());
            return safe.toString();
        } catch (final Exception ignored) {
            return "<unparseable-url>";
        }
    }
}
