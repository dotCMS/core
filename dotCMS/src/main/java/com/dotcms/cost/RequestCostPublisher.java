package com.dotcms.cost;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.codec.digest.DigestUtils;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

/**
 * Retains request-cost snapshots in the tenant database and retries their immutable bodies until
 * insertion is confirmed. Activates when both REQUEST_COST_PUSH_URL and REQUEST_COST_PUSH_TOKEN
 * are configured; disabling publishing pauses delivery without deleting pending work.
 */
@ApplicationScoped
public class RequestCostPublisher {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int FAIL_LOG_INTERVAL_MS = 10 * 60 * 1000;
    private final AtomicBoolean httpSchemeWarned = new AtomicBoolean(false);
    private final RequestCostOutbox outbox = new RequestCostOutbox();
    private ScheduledExecutorService scheduler;
    private long lastStatisticsNanos;

    /** Starts recovery at application initialization, even before fresh customer traffic arrives. */
    public void start(@Observes @Initialized(ApplicationScoped.class) final Object event) {
        scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
            final Thread thread = new Thread(task, "RequestCostDelivery");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::drain, 1, 1, TimeUnit.SECONDS);
    }

    /** Stops polling; unfinished transactions leave their bodies pending. */
    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    /** Preserves the existing URL-and-sanitized-token activation gate. */
    public boolean isEnabled() {
        return UtilMethods.isSet(getUrl()) && UtilMethods.isSet(sanitizeHeaderValue(getToken()));
    }

    /** Reads the operator-configured destination without caching credentials or routing. */
    private String getUrl() {
        return Config.getStringProperty("REQUEST_COST_PUSH_URL", null);
    }

    /** Resolves the current bearer credential at send time. */
    private String getToken() {
        return Config.getStringProperty("REQUEST_COST_PUSH_TOKEN", null);
    }

    /** Keeps configured waits finite; the existing default remains five seconds. */
    private long getTimeoutMs() {
        return Math.max(1L, Math.min(60_000L,
                Config.getLongProperty("REQUEST_COST_PUSH_TIMEOUT_MS", 5_000L)));
    }

    /** Serializes once when the monitor captures a window, not on each delivery attempt. */
    static String serializeSnapshot(final RequestCostSnapshot snapshot) throws JsonProcessingException {
        return MAPPER.writeValueAsString(snapshot);
    }

    /** Captures a non-secret destination identity alongside the immutable window. */
    public String destinationFingerprint() {
        final String url = getUrl();
        return UtilMethods.isSet(url) ? DigestUtils.sha256Hex(url) : null;
    }

    /**
     * Durably admits an already-frozen body only for its captured destination. A false result
     * leaves captured counters owned by the monitor, including when the configured URL changes.
     */
    public boolean publish(final String destination, final String payload) {
        if (!isEnabled()) {
            return false;
        }
        final String url = getUrl();
        if (!validDestination(url) || !DigestUtils.sha256Hex(url).equals(destination)) {
            return false;
        }
        try {
            return outbox.add(url, payload);
        } catch (final Exception e) {
            Logger.warnEvery(getClass(), "REQUEST_COST_OUTBOX_ADMISSION",
                    "Request cost persistence unconfirmed; captured window retained in memory",
                    FAIL_LOG_INTERVAL_MS);
            return false;
        }
    }

    /** Drains one durable record per poll, independently of the request-cost monitor. */
    private void drain() {
        try {
            if (!isEnabled()) {
                return;
            }
            final String url = getUrl();
            if (!validDestination(url)) {
                return;
            }
            outbox.deliver(url, payload -> post(url, payload));
            final long now = System.nanoTime();
            if (lastStatisticsNanos == 0
                    || now - lastStatisticsNanos >= TimeUnit.MILLISECONDS.toNanos(FAIL_LOG_INTERVAL_MS)) {
                outbox.logStatistics();
                lastStatisticsNanos = now;
            }
        } catch (final Exception e) {
            Logger.warnEvery(getClass(), "REQUEST_COST_OUTBOX_DELIVERY",
                    "Request cost outbox unavailable; durable work retained for retry",
                    FAIL_LOG_INTERVAL_MS);
        }
    }

    /** Returns a status only after consuming the complete bounded response; uncertainty is -1. */
    private int post(final String url, final String payload) {
        final String token = sanitizeHeaderValue(getToken());
        if (!url.equals(getUrl()) || !UtilMethods.isSet(token)) {
            return -1;
        }
        warnOncePlainHttp(url);
        try {
            final CircuitBreakerUrl call = CircuitBreakerUrl.builder()
                    .setMethod(CircuitBreakerUrl.Method.POST)
                    .setUrl(url)
                    .setHeaders(Map.of("Authorization", "Bearer " + token))
                    .setRawData(payload)
                    .setTimeout(getTimeoutMs())
                    .setAllowRedirects(false)
                    .setThrowWhenError(false)
                    .setRaiseFailsafe(true)
                    .setMaxResponseBytes(64 * 1024)
                    .build();
            // doString logs error bodies; discard them instead so credentials cannot be echoed.
            call.doOutUnmetered(OutputStream.nullOutputStream());
            return call.isProcessed() ? call.response() : -1;
        } catch (final Exception e) {
            Logger.warnEvery(getClass(), "REQUEST_COST_PUSH_TRANSPORT",
                    "Request cost delivery did not complete; body retained for retry",
                    FAIL_LOG_INTERVAL_MS);
            return -1;
        }
    }

    /** Refuses URL-embedded credentials that the shared HTTP client's own diagnostics could log. */
    private boolean validDestination(final String url) {
        if (!UtilMethods.isSet(url)) {
            return false;
        }
        try {
            final URI uri = URI.create(url);
            if (uri.getHost() != null && ("https".equalsIgnoreCase(uri.getScheme())
                    || "http".equalsIgnoreCase(uri.getScheme())) && uri.getRawUserInfo() == null
                    && uri.getRawQuery() == null && uri.getRawFragment() == null) {
                return true;
            }
        } catch (final IllegalArgumentException ignored) {
            // Invalid configuration is reported without echoing a possibly secret-bearing URL.
        }
        Logger.warnEvery(getClass(), "REQUEST_COST_PUSH_CONFIG",
                "Request cost destination must be HTTP(S) without userinfo, query, or fragment; work retained",
                FAIL_LOG_INTERVAL_MS);
        return false;
    }

    /** Warns once if a legacy plain-HTTP destination would expose its bearer token. */
    private void warnOncePlainHttp(final String url) {
        if (httpSchemeWarned.get()) {
            return;
        }
        if ("http".equalsIgnoreCase(URI.create(url).getScheme())
                && httpSchemeWarned.compareAndSet(false, true)) {
            Logger.warn(getClass(), "REQUEST_COST_PUSH_URL uses plain http://; use https:// "
                    + "for any non-loopback destination to protect the bearer token.");
        }
    }

    /** Strips CR/LF and surrounding whitespace to prevent header injection. */
    static String sanitizeHeaderValue(final String value) {
        return value == null ? null : value.replace("\r", "").replace("\n", "").trim();
    }

    /** Removes URL userinfo before a caller uses the URL in diagnostics. */
    static String sanitizeUrlForLog(final String url) {
        if (!UtilMethods.isSet(url)) {
            return url;
        }
        try {
            final URI uri = URI.create(url);
            if (uri.getUserInfo() == null) {
                return url;
            }
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(),
                    uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
        } catch (final Exception ignored) {
            return "<unparseable-url>";
        }
    }
}
