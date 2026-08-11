package com.dotcms.health.checks.cdi;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.health.config.HealthCheckConfig.HealthCheckMode;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.health.util.HealthCheckBase;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.cache.transport.NullTransport;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;

/**
 * Health check for the cluster cache-invalidation transport (pub/sub).
 *
 * A node whose cache transport is not initialized silently drops every cache invalidation it
 * tries to send, so other nodes serve stale content while all standard probes report healthy
 * (see issue #36803 / incident #36544). This check surfaces that state:
 *
 * - transport not initialized -> unhealthy
 * - cluster rewire failing repeatedly -> unhealthy (see {@link #DEFAULT_REWIRE_FAILURE_THRESHOLD})
 * - dropped and failed invalidation counts exposed in structured data for monitoring
 *
 * Failed invalidations (initialized transport whose sends are erroring) are reported but do not
 * on their own make the check unhealthy: the count is cumulative and never resets, so alarming on
 * "greater than zero" would pin a node DOWN forever after one transient publish error. Alert on
 * the rate of increase of {@code dotcms.cache.transport.invalidations.failed} instead.
 *
 * Nodes with no real transport -- {@link NullTransport} via
 * {@code CACHE_INVALIDATION_TRANSPORT_CLASS}, or no resolvable cache layer at all -- always
 * report healthy, since there is nothing that could be dropping invalidations.
 *
 * Readiness: by default this check runs in MONITOR_MODE, so it reports degradation but never
 * fails readiness probes -- a node that cannot send invalidations can still serve traffic, and
 * gating readiness on the transport could deadlock a cold cluster start where the transport
 * initializes after traffic begins. Operators who prefer to drain such nodes can opt in with
 * {@code health.check.cache-transport.mode=PRODUCTION}.
 *
 * Configuration Properties:
 * - health.check.cache-transport.mode = Safety mode (PRODUCTION, MONITOR_MODE, DISABLED)
 * - health.check.cache-transport.rewire-failure-threshold = consecutive rewire failures
 *   tolerated before reporting unhealthy (default 3)
 */
@ApplicationScoped
public class CacheTransportHealthCheck extends HealthCheckBase {

    /**
     * Consecutive rewire failures tolerated before this check reports unhealthy.
     *
     * A single failure is usually a transient blip: {@code testCluster()} can throw on a
     * momentary database hiccup while the transport itself stays initialized and invalidations
     * keep flowing. Alarming on the first failure would report DOWN for a working node purely
     * from a counter that has not been reset yet. {@code ClusterFactory.rewireClusterIfNeeded()}
     * retries on every server heartbeat (60s by default) and zeroes the counter on success, so
     * reaching this threshold means the rewire has been failing for minutes, not milliseconds.
     */
    private static final int DEFAULT_REWIRE_FAILURE_THRESHOLD = 3;

    /**
     * Transport state resolved once per {@code check()} invocation.
     *
     * {@code HealthCheckBase.check()} calls {@link #performCheck()} and then
     * {@link #buildStructuredData} sequentially on the same thread, so the snapshot is handed
     * between them here instead of resolving the transport and re-reading its counters twice.
     * Beyond the wasted work, two independent reads let a single health response contradict
     * itself -- a message saying the transport is initialized alongside structured data saying
     * it is not, because the state moved in between. A ThreadLocal (rather than a field) keeps
     * concurrent liveness/readiness polls of this {@code @ApplicationScoped} bean isolated, and
     * {@link #buildStructuredData} always clears it.
     */
    private static final ThreadLocal<TransportSnapshot> CURRENT_SNAPSHOT = new ThreadLocal<>();

    @Override
    protected CheckResult performCheck() throws Exception {

        if (isShutdownInProgress()) {
            CURRENT_SNAPSHOT.set(TransportSnapshot.unavailable(0L));
            return new CheckResult(true, 0L, "Cache transport check skipped during shutdown");
        }

        final TransportSnapshot snapshot = snapshot();
        CURRENT_SNAPSHOT.set(snapshot);

        if (!snapshot.present) {
            return new CheckResult(true, 0L,
                    "No cluster cache transport in use (" + snapshot.transportName + ")");
        }

        if (!snapshot.initialized) {
            return new CheckResult(false, 0L,
                    "Cache transport is NOT initialized - cluster cache invalidations are being dropped"
                            + " (dropped so far: " + snapshot.dropped
                            + ", consecutive rewire failures: " + snapshot.rewireFailures + ")");
        }

        final int threshold = getConfigProperty("rewire-failure-threshold",
                DEFAULT_REWIRE_FAILURE_THRESHOLD);

        if (snapshot.rewireFailures >= threshold) {
            return new CheckResult(false, 0L,
                    "Cluster rewire has failed " + snapshot.rewireFailures
                            + " consecutive times (threshold: " + threshold
                            + ") - the cache transport may be stale");
        }

        return new CheckResult(true, 0L,
                "Cache transport initialized (" + snapshot.transportName
                        + ", dropped invalidations: " + snapshot.dropped
                        + ", failed invalidations: " + snapshot.failed
                        + ", consecutive rewire failures: " + snapshot.rewireFailures + ")");
    }

    /**
     * Reads every piece of transport state this check reports on, exactly once.
     */
    private TransportSnapshot snapshot() {

        final long rewireFailures = ClusterFactory.getRewireFailures();
        final CacheTransport transport = getTransport();

        if (transport == null) {
            return TransportSnapshot.unavailable(rewireFailures);
        }

        // NullTransport is a deliberate no-op transport (single node, or
        // CACHE_INVALIDATION_TRANSPORT_CLASS pointed at it). Its isInitialized() is false
        // whenever it has been shut down, which must not be read as dropped invalidations.
        final boolean present = !(transport instanceof NullTransport);

        return new TransportSnapshot(transport.getClass().getSimpleName(), present,
                transport.isInitialized(), transport.getDroppedMessages(),
                transport.getFailedMessages(), rewireFailures);
    }

    /**
     * Resolves the transport through the {@code DotCacheAdministrator} interface.
     *
     * Deliberately not {@code getImplementationObject()} plus a cast to
     * {@code ChainableCacheAdministratorImpl}: {@code getTransport()} is part of the interface
     * and {@code CommitListenerCacheWrapper} delegates it straight through, whereas the cast
     * throws {@code ClassCastException} for any other administrator implementation --
     * {@code NullCacheAdministrator.getImplementationObject()} returns itself, which is the
     * unit-test path. Compare {@code ClusterResource}, which uses the interface method.
     */
    private CacheTransport getTransport() {
        try {
            return CacheLocator.getCacheAdministrator().getTransport();
        } catch (Exception e) {
            // cache layer not up yet - nothing to report on
            return null;
        }
    }

    @Override
    public String getName() {
        return "cache-transport";
    }

    @Override
    protected HealthCheckMode getDefaultMode() {
        return HealthCheckMode.MONITOR_MODE;
    }

    @Override
    public int getOrder() {
        return 35; // right after the cache check
    }

    /**
     * Never liveness - a broken transport must not restart pods (that made #36544 worse)
     */
    @Override
    public boolean isLivenessCheck() {
        return false;
    }

    /**
     * Participates in readiness reporting, but the default MONITOR_MODE means it will not
     * fail probes unless an operator opts in to PRODUCTION mode.
     */
    @Override
    public boolean isReadinessCheck() {
        return getMode() != HealthCheckMode.DISABLED;
    }

    @Override
    public String getDescription() {
        return String.format(
                "Verifies the cluster cache-invalidation transport is initialized and not dropping messages (Mode: %s)",
                getMode().name());
    }

    @Override
    protected Map<String, Object> buildStructuredData(CheckResult result, HealthStatus originalStatus,
                                                      HealthStatus finalStatus, HealthCheckMode mode) {
        try {
            TransportSnapshot snapshot = CURRENT_SNAPSHOT.get();
            if (snapshot == null) {
                // performCheck() threw before it could snapshot - resolve once here instead
                snapshot = snapshot();
            }

            final Map<String, Object> data = new HashMap<>();
            data.put("transport", snapshot.transportName);
            data.put("rewireFailures", snapshot.rewireFailures);
            if (snapshot.present) {
                data.put("initialized", snapshot.initialized);
                data.put("droppedInvalidations", snapshot.dropped);
                data.put("failedInvalidations", snapshot.failed);
            }
            return data;
        } finally {
            CURRENT_SNAPSHOT.remove();
        }
    }

    /**
     * Immutable, single-read view of the transport state, so the message and the structured
     * data of one health response can never disagree.
     */
    private static final class TransportSnapshot {

        private static final String NO_TRANSPORT = "none";

        final String transportName;
        /** true only for a real transport that is expected to carry invalidations */
        final boolean present;
        final boolean initialized;
        final long dropped;
        final long failed;
        final long rewireFailures;

        TransportSnapshot(final String transportName, final boolean present, final boolean initialized,
                          final long dropped, final long failed, final long rewireFailures) {
            this.transportName = transportName;
            this.present = present;
            this.initialized = initialized;
            this.dropped = dropped;
            this.failed = failed;
            this.rewireFailures = rewireFailures;
        }

        static TransportSnapshot unavailable(final long rewireFailures) {
            return new TransportSnapshot(NO_TRANSPORT, false, false, 0L, 0L, rewireFailures);
        }
    }
}
