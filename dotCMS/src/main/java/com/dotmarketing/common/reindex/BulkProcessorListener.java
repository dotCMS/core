package com.dotmarketing.common.reindex;

import static com.dotcms.content.index.IndexConfigHelper.logShadowWriteFailure;

import com.dotcms.content.index.IndexConfigHelper;
import com.dotcms.content.index.IndexTag;
import com.dotcms.content.index.domain.IndexBulkItemResult;
import com.dotcms.content.index.domain.IndexBulkListener;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link IndexBulkListener} that handles the business logic before/after reindexing content.
 *
 * <p>This class contains no vendor-specific imports: it receives neutral
 * {@link IndexBulkItemResult} values from the active
 * {@link com.dotcms.content.index.ContentletIndexOperations} adapter, which is
 * responsible for mapping library-specific bulk-response types.</p>
 *
 * @author nollymar
 */
public class BulkProcessorListener implements IndexBulkListener {

    final Map<String, ReindexEntry> workingRecords;

    static final List<String> RESERVED_IDS = List.of(Host.SYSTEM_HOST);

    private volatile long contentletsIndexed;
    private int lastBatchSize;

    /** Provider identity — used for log labels and config gate. */
    private final IndexTag provider;
    /**
     * {@code true} for OS entries in dual-write phases (fire-and-forget).
     * These listeners log failures as warnings but never touch the reindex queue
     * or trigger a bulk-processor rebuild.
     */
    private final boolean shadow;

    /**
     * Creates the primary listener for the current migration phase.
     * Phase 3 (OS only) labels itself {@link IndexTag#OS}; all other phases label
     * themselves {@link IndexTag#ES} (ES is primary or dual-write leader).
     */
    BulkProcessorListener() {
        this(IndexConfigHelper.MigrationPhase.current().isMigrationComplete()
                ? IndexTag.OS : IndexTag.ES, false);
    }

    private BulkProcessorListener(final IndexTag provider, final boolean shadow) {
        this.workingRecords = new ConcurrentHashMap<>();
        this.provider = provider;
        this.shadow = shadow;
    }

    /**
     * Creates a listener for a shadow-index provider (OS in Phases 1 and 2).
     *
     * <p>The shadow index replicates ES writes but is not yet the source of truth.
     * Its failure semantics are fire-and-forget: failures are logged at warn level
     * but the reindex queue entry is never marked as failed and no rebuild is triggered.
     * In Phase 3, OS becomes the primary and this factory is no longer used — the caller
     * passes the standard {@link BulkProcessorListener} directly.</p>
     */
    public static BulkProcessorListener forShadowProvider(final IndexTag provider) {
        return new BulkProcessorListener(provider, true);
    }

    public long getContentletsIndexed() {
        return contentletsIndexed;
    }

    @Override
    public void beforeBulk(final long executionId, final int actionCount) {
        this.lastBatchSize = actionCount;
        contentletsIndexed += actionCount;
        // Per-provider log visibility: REINDEX_BULK_LOG_ES_PROVIDER / REINDEX_BULK_LOG_OS_PROVIDER
        if (!Config.getBooleanProperty("REINDEX_BULK_LOG_" + provider.name() + "_PROVIDER", true)) {
            return;
        }
        final String tag = "[" + provider.name() + "] ";
        final String serverId = APILocator.getServerAPI().readServerId();
        final List<String> servers = Try.of(
                () -> APILocator.getServerAPI().getReindexingServers())
                .getOrElse(List.of(serverId));
        Logger.info(this.getClass(), "-----------");
        Logger.info(this.getClass(), tag + "Reindexing Server #  : "
                + (servers.indexOf(serverId) + 1) + " of " + servers.size());
        Logger.info(this.getClass(), tag + "Total Indexed        : " + contentletsIndexed);
        if (!shadow) {
            Logger.info(this.getClass(), tag + "ReindexEntries found : " + workingRecords.size());
        }
        Logger.info(this.getClass(), tag + "BulkRequests created : " + actionCount);
        final Optional<String> duration = APILocator.getContentletIndexAPI().reindexTimeElapsed();
        duration.ifPresent(d -> Logger.info(this, tag + "Full Reindex Elapsed : " + d));
        Logger.info(this.getClass(), "-----------");
    }

    @Override
    public void afterBulk(final long executionId, final List<IndexBulkItemResult> results) {
        if (shadow) {
            // OS shadow — fire-and-forget; log individual failures for observability only
            results.stream()
                    .filter(IndexBulkItemResult::failed)
                    .forEach(r -> logShadowWriteFailure(this.getClass(),
                            "[OS] Index failure (fire-and-forget): " + r.failureMessage(), null));
            return;
        }
        Logger.debug(this.getClass(), "Bulk process completed");
        final List<ReindexEntry> successful = new ArrayList<>();
        float totalResponses = 0;

        for (final IndexBulkItemResult result : results) {
            totalResponses++;
            final String reservedId = getMatchingReservedIdIfAny(result.id());
            final String id;
            if (reservedId != null) {
                id = reservedId;
            } else {
                final int sep = result.id().indexOf(StringPool.UNDERLINE);
                id = sep > 0 ? result.id().substring(0, sep) : result.id();
            }

            final ReindexEntry idx = workingRecords.get(id);
            if (idx == null) {
                continue;
            }
            if (result.failed()) {
                handleFailure(idx, "bulk index failure:" + result.failureMessage());
            } else {
                successful.add(idx);
            }
        }

        handleSuccess(successful);
        // 50% failure rate guard: log a warning so the failure is observable.
        // No explicit rebuild needed — ReindexThread creates a fresh processor per batch,
        // so the next batch will automatically start with a clean processor.
        if (lastBatchSize > 0 && (totalResponses == 0 || ((float) successful.size() / totalResponses < .5))) {
            Logger.warn(this.getClass(),
                    "High bulk-index failure rate detected (>50%) — next batch will use a fresh processor.");
        }
    }

    @Override
    public void afterBulk(final long executionId, final Throwable failure) {
        final String msg = failure != null ? failure.getMessage() : "(no message)";
        if (shadow) {
            logShadowWriteFailure(this.getClass(),
                    "[OS] Bulk process failed entirely (fire-and-forget): " + msg, failure);
            return;
        }
        Logger.error(ReindexThread.class, "Bulk process failed entirely: " + msg, failure);
        workingRecords.values().forEach(idx -> handleFailure(idx, msg));
    }

    static String getMatchingReservedIdIfAny(final String id) {
        for (final String reservedId : RESERVED_IDS) {
            if (id.contains(reservedId)) {
                return reservedId;
            }
        }
        return null;
    }

    private void handleSuccess(final List<ReindexEntry> successful) {
        try {
            if (!successful.isEmpty()) {
                APILocator.getReindexQueueAPI().deleteReindexEntry(successful);
                CacheLocator.getESQueryCache().clearCache();
                CacheLocator.getOSQueryCache().clearCache();
            }
        } catch (DotDataException e) {
            Logger.warnAndDebug(this.getClass(),
                    "unable to delete indexjournal: " + e.getMessage(), e);
        }
    }

    private void handleFailure(final ReindexEntry idx, final String cause) {
        try {
            APILocator.getReindexQueueAPI().markAsFailed(idx, cause);
        } catch (DotDataException e) {
            Logger.warnAndDebug(this.getClass(),
                    "unable to requeue indexjournal: " + idx, e);
        }
    }
}
