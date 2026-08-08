package com.dotcms.health.checks.cdi;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.health.config.HealthCheckConfig.HealthCheckMode;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.health.util.HealthCheckBase;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.ChainableCacheAdministratorImpl;
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
 * - persistent cluster rewire failures -> unhealthy
 * - dropped-invalidation count exposed in structured data for monitoring
 *
 * Single-node / community installs use {@link NullTransport} (or no transport) and always
 * report healthy.
 *
 * Readiness: by default this check runs in MONITOR_MODE, so it reports degradation but never
 * fails readiness probes — a node that cannot send invalidations can still serve traffic, and
 * gating readiness on the transport could deadlock a cold cluster start where the transport
 * initializes after traffic begins. Operators who prefer to drain such nodes can opt in with
 * {@code health.check.cache-transport.mode=PRODUCTION}.
 *
 * Configuration Properties:
 * - health.check.cache-transport.mode = Safety mode (PRODUCTION, MONITOR_MODE, DISABLED)
 */
@ApplicationScoped
public class CacheTransportHealthCheck extends HealthCheckBase {

    @Override
    protected CheckResult performCheck() throws Exception {

        if (isShutdownInProgress()) {
            return new CheckResult(true, 0L, "Cache transport check skipped during shutdown");
        }

        final CacheTransport transport = getTransport();

        if (transport == null || transport instanceof NullTransport) {
            return new CheckResult(true, 0L, "No cluster cache transport in use (single node / community)");
        }

        final long rewireFailures = ClusterFactory.getRewireFailures();
        final long dropped = transport.getDroppedMessages();

        if (!transport.isInitialized()) {
            return new CheckResult(false, 0L,
                    "Cache transport is NOT initialized - cluster cache invalidations are being dropped"
                            + " (dropped so far: " + dropped
                            + ", consecutive rewire failures: " + rewireFailures + ")");
        }

        if (rewireFailures > 0) {
            return new CheckResult(false, 0L,
                    "Cluster rewire is failing (consecutive failures: " + rewireFailures
                            + ") - cache transport may be stale");
        }

        return new CheckResult(true, 0L,
                "Cache transport initialized (" + transport.getClass().getSimpleName()
                        + ", dropped invalidations: " + dropped + ")");
    }

    private CacheTransport getTransport() {
        try {
            return ((ChainableCacheAdministratorImpl) CacheLocator.getCacheAdministrator()
                    .getImplementationObject()).getTransport();
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
        final Map<String, Object> data = new HashMap<>();
        final CacheTransport transport = getTransport();
        if (transport != null) {
            data.put("transport", transport.getClass().getSimpleName());
            data.put("initialized", transport.isInitialized());
            data.put("droppedInvalidations", transport.getDroppedMessages());
        }
        data.put("rewireFailures", ClusterFactory.getRewireFailures());
        return data;
    }
}
