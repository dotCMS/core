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
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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

    /**
     * Config key: minutes between two systemic-rejection escalations
     * ({@link #systemicFailureEscalation(IndexTag, int, Map)}). Default: 10.
     */
    static final String SHADOW_ESCALATION_INTERVAL_KEY =
            "REINDEX_SHADOW_FAILURE_ESCALATION_MINUTES";

    /** Stand-in used when the vendor reports a failed item without any message. */
    static final String NO_FAILURE_MESSAGE = "(no failure message reported)";

    /**
     * Epoch millis of the last systemic-rejection escalation. Static on purpose: a fresh listener is
     * created for every batch, so per-instance state could not rate-limit anything.
     */
    private static final AtomicLong LAST_SYSTEMIC_ESCALATION = new AtomicLong(0L);

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
            // OS shadow — fire-and-forget, but summarised: a systemic rejection (permissions, TLS,
            // unreachable cluster) repeats verbatim for every document in the batch, so logging one
            // line per failed item buried the log — a real reindex emits hundreds of thousands of
            // identical entries, hiding every other line including the actionable one below
            // (observed on issue #36222, TC-056: ~900 identical WARNs in one minute).
            logShadowBatchFailures(results);
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

    /**
     * Logs the failed items of a shadow bulk batch as one aggregated entry per distinct failure
     * message, keeps the per-item detail at {@code DEBUG}, and escalates once when the whole batch
     * was rejected for a systemic reason.
     *
     * @param results every item result of the completed batch, successful ones included
     */
    private void logShadowBatchFailures(final List<IndexBulkItemResult> results) {
        final Map<String, Long> failuresByMessage = summarizeFailures(results);
        if (failuresByMessage.isEmpty()) {
            return;
        }
        final String tag = "[" + provider.name() + "] ";
        failuresByMessage.forEach((message, count) -> logShadowWriteFailure(this.getClass(),
                tag + "Index failure (fire-and-forget): " + count + " of " + results.size()
                        + " item(s) in this batch — " + message, null));

        if (Logger.isDebugEnabled(this.getClass())) {
            results.stream()
                    .filter(IndexBulkItemResult::failed)
                    .forEach(result -> Logger.debug(this.getClass(), tag + "Failed item id="
                            + result.id() + ": " + result.failureMessage()));
        }

        final Optional<String> escalation =
                systemicFailureEscalation(provider, results.size(), failuresByMessage);
        if (escalation.isPresent() && shouldEscalateNow()) {
            Logger.error(this.getClass(), escalation.get());
        }
    }

    /**
     * Counts the failed items of a batch grouped by failure message, preserving first-seen order.
     * A systemic cause produces a single entry whose count equals the batch size; a per-document
     * cause produces several entries, or one entry that covers only part of the batch.
     *
     * @param results every item result of the completed batch
     * @return failure message → number of items that failed with it; empty when nothing failed
     */
    static Map<String, Long> summarizeFailures(final List<IndexBulkItemResult> results) {
        final Map<String, Long> failuresByMessage = new LinkedHashMap<>();
        for (final IndexBulkItemResult result : results) {
            if (result.failed()) {
                final String message = result.failureMessage();
                failuresByMessage.merge(UtilMethods.isSet(message) ? message : NO_FAILURE_MESSAGE,
                        1L, Long::sum);
            }
        }
        return failuresByMessage;
    }

    /**
     * Builds the actionable message for a shadow batch that was rejected <em>in full</em> for a
     * systemic reason — the state where the shadow store silently stops receiving writes and
     * diverges from the authoritative one, which must never be promoted by advancing the migration
     * phase (issue #36222 follow-up: index creation was already covered by
     * {@code ContentletIndexAPIImpl.handleOsBootstrapFailure}, the write path was not).
     *
     * <p>Deliberately silent unless <em>every</em> item failed: a partial failure is a per-document
     * problem, and unclassifiable messages (mapping conflicts) are not migration blockers either.
     * The phase is not reset here — a bulk rejection can be scoped to a single index, so the
     * decision to halt is left to the operator, with this line and the readiness report as the
     * signal.</p>
     *
     * @param provider          the shadow provider that rejected the batch
     * @param batchSize         total items in the batch, successful ones included
     * @param failuresByMessage output of {@link #summarizeFailures(List)}
     * @return the escalation message, or empty when this batch does not warrant one
     */
    static Optional<String> systemicFailureEscalation(final IndexTag provider, final int batchSize,
            final Map<String, Long> failuresByMessage) {
        final long failed = failuresByMessage.values().stream().mapToLong(Long::longValue).sum();
        if (batchSize <= 0 || failed < batchSize) {
            return Optional.empty();
        }
        return failuresByMessage.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .flatMap(dominant -> IndexConfigHelper
                        .systemicFailureRemediation(dominant.getKey())
                        .map(remediation -> "[" + provider.name() + "] EVERY document in this bulk"
                                + " batch (" + batchSize + " item(s)) was rejected by "
                                + provider.name() + " — likelyCause=" + remediation
                                + ". The shadow store is NOT receiving writes and is diverging from"
                                + " the authoritative store, so it must not be promoted: advancing"
                                + " the migration phase would serve reads from a stale index."
                                + " Fix the cause above, then run a full reindex to resynchronise."
                                + " Rejection: " + dominant.getKey()));
    }

    /**
     * Rate-limits the systemic escalation to one entry per
     * {@value #SHADOW_ESCALATION_INTERVAL_KEY} minutes. Static because a fresh listener is built
     * for every batch, so per-instance state would not throttle anything.
     *
     * @return {@code true} when this caller won the right to log the escalation now
     */
    private static boolean shouldEscalateNow() {
        final long intervalMs = TimeUnit.MINUTES.toMillis(
                Config.getIntProperty(SHADOW_ESCALATION_INTERVAL_KEY, 10));
        final long now = System.currentTimeMillis();
        final long previous = LAST_SYSTEMIC_ESCALATION.get();
        if (previous != 0L && now - previous < intervalMs) {
            return false;
        }
        return LAST_SYSTEMIC_ESCALATION.compareAndSet(previous, now);
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
