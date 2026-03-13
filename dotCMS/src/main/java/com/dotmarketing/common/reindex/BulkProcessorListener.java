package com.dotmarketing.common.reindex;

import com.dotcms.content.index.domain.IndexBulkItemResult;
import com.dotcms.content.index.domain.IndexBulkListener;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    private long contentletsIndexed;

    BulkProcessorListener() {
        this.workingRecords = new HashMap<>();
    }

    public long getContentletsIndexed() {
        return contentletsIndexed;
    }

    @Override
    public void beforeBulk(final long executionId, final int actionCount) {
        final String serverId = APILocator.getServerAPI().readServerId();
        final List<String> servers = Try.of(
                () -> APILocator.getServerAPI().getReindexingServers())
                .getOrElse(List.of(serverId));
        Logger.info(this.getClass(), "-----------");
        Logger.info(this.getClass(), "Reindexing Server #  : "
                + (servers.indexOf(serverId) + 1) + " of " + servers.size());
        Logger.info(this.getClass(), "Total Indexed        : " + contentletsIndexed);
        Logger.info(this.getClass(), "ReindexEntries found : " + workingRecords.size());
        Logger.info(this.getClass(), "BulkRequests created : " + actionCount);
        contentletsIndexed += actionCount;
        final Optional<String> duration = APILocator.getContentletIndexAPI().reindexTimeElapsed();
        if (duration.isPresent()) {
            Logger.info(this, "Full Reindex Elapsed : " + duration.get());
        }
        Logger.info(this.getClass(), "-----------");
    }

    @Override
    public void afterBulk(final long executionId, final List<IndexBulkItemResult> results) {
        Logger.debug(this.getClass(), "Bulk process completed");
        final List<ReindexEntry> successful = new ArrayList<>();
        float totalResponses = 0;

        for (final IndexBulkItemResult result : results) {
            totalResponses++;
            final String reservedId = getMatchingReservedIdIfAny(result.id());
            final String id = reservedId != null
                    ? reservedId
                    : result.id().substring(0, result.id().indexOf(StringPool.UNDERLINE));

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
        // 50% failure rate forces a rebuild of the BulkProcessor
        if (totalResponses == 0 || (successful.size() / totalResponses < .5)) {
            ReindexThread.rebuildBulkIndexer();
        }
    }

    @Override
    public void afterBulk(final long executionId, final Throwable failure) {
        Logger.error(ReindexThread.class,
                "Bulk process failed entirely: " + failure.getMessage(), failure);
        workingRecords.values().forEach(idx -> handleFailure(idx, failure.getMessage()));
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
