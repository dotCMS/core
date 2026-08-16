package com.dotcms.health.checks.cdi;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImpl;
import com.dotcms.content.elasticsearch.business.ReindexMappingRunner;
import com.dotcms.health.config.HealthCheckConfig.HealthCheckMode;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.health.util.HealthCheckBase;
import com.dotmarketing.util.Logger;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;

/**
 * Reports whether background content indexing is able to make progress.
 *
 * <p>The mapping guard walks away from tasks that hang reading binary fields from storage. While
 * every one of those abandoned tasks is still stuck, no new content can be mapped — the queue is
 * intact and nothing is lost, but the index stops receiving updates. That state used to be
 * completely invisible: pages kept rendering, so an instance could go days with content saved to
 * the database and absent from the index (issue #37038). This check makes it observable.</p>
 *
 * <p>Not a liveness check: the guard recovers on its own when storage answers again, so a restart
 * is neither required nor helpful.</p>
 */
@ApplicationScoped
public class ReindexMappingHealthCheck extends HealthCheckBase {

    @Override
    protected CheckResult performCheck() throws Exception {
        if (isShutdownInProgress()) {
            Logger.debug(this, "Skipping reindex mapping health check during shutdown");
            return new CheckResult(false, 0L,
                    "Reindex mapping health check skipped during shutdown");
        }
        return measureExecution(() -> {
            final ReindexMappingRunner.Status status =
                    ContentletIndexAPIImpl.sharedMappingRunner().status();
            if (status.degraded()) {
                throw new Exception("Content indexing is stalled: all " + status.maxAbandoned()
                        + " abandoned mapping slots are still stuck, so no content can be indexed"
                        + " until the underlying storage (or the index bulk endpoint) answers"
                        + " again. Queued work is intact and will resume automatically.");
            }
            if (status.abandoned() > 0) {
                return "Content indexing is progressing with " + status.abandoned() + " of "
                        + status.maxAbandoned() + " mapping slots stuck on unresponsive storage";
            }
            return "Content indexing mapping pool healthy";
        });
    }

    @Override
    public String getName() {
        return "reindex-mapping";
    }

    @Override
    protected HealthCheckMode getDefaultMode() {
        return HealthCheckMode.MONITOR_MODE;
    }

    @Override
    public int getOrder() {
        return 45; // Just after the search dependency it feeds
    }

    /**
     * NOT safe for liveness — the guard self-heals, so restarting the node fixes nothing that
     * waiting would not.
     */
    @Override
    public boolean isLivenessCheck() {
        return false;
    }

    @Override
    public boolean isReadinessCheck() {
        return getMode() != HealthCheckMode.DISABLED;
    }

    @Override
    public String getDescription() {
        return "Verifies that background content indexing can map and enqueue new content";
    }

    @Override
    protected Map<String, Object> buildStructuredData(final CheckResult result,
            final HealthStatus originalStatus, final HealthStatus finalStatus,
            final HealthCheckMode mode) {
        final Map<String, Object> data = new HashMap<>();
        final ReindexMappingRunner.Status status =
                ContentletIndexAPIImpl.sharedMappingRunner().status();
        data.put("inFlightMappings", status.inFlight());
        data.put("maxConcurrentMappings", status.maxConcurrent());
        data.put("abandonedMappings", status.abandoned());
        data.put("maxAbandonedMappings", status.maxAbandoned());
        data.put("indexingStalled", status.degraded());
        if (result.error != null) {
            data.put("errorType", "reindex_mapping_stalled");
        }
        return data;
    }
}
