package com.dotmarketing.common.reindex;


import com.dotmarketing.beans.Host;
import com.google.common.collect.ImmutableList;
import com.dotmarketing.business.APILocator;

import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;

import io.vavr.control.Try;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;


/**
 * {@link BulkProcessor.Listener} that handles the logic before/after reindexing content
 * @author nollymar
 */
public class BulkProcessorListener implements BulkProcessor.Listener {

    final Map<String, ReindexEntry> workingRecords;

    static final List<String> RESERVED_IDS = List.of(Host.SYSTEM_HOST);

    private long contentletsIndexed;

    AtomicInteger totalWorkingRecords = new AtomicInteger(0);
    AtomicInteger totalResponses = new AtomicInteger(0);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);

    BlockingQueue<ReindexResult> queue = new LinkedBlockingQueue<>();

    BulkProcessorListener () {
        this.workingRecords = new ConcurrentHashMap<>();
    }

    public long getContentletsIndexed(){
        return contentletsIndexed;
    }

    public int getTotalResponses(){
        return totalResponses.get();
    }

    public int getSuccessCount(){
        return successCount.get();
    }
    public int getFailureCount(){
        return failureCount.get();
    }
    public Map<String, ReindexEntry> getWorkingRecords(){
        return workingRecords;
    }

    public BlockingQueue<ReindexResult> getQueue(){
        return queue;
    }

    public void addWorkingRecord(Map<String,ReindexEntry> entries) {
        for ( Map.Entry<String,ReindexEntry> entrySet : entries.entrySet()) {
            ReindexEntry previousEntry = workingRecords.put(entrySet.getKey(), entrySet.getValue());
            if (previousEntry == null) {
                totalWorkingRecords.incrementAndGet();
            } else {
                Logger.warn(this.getClass(), "ReindexEntry already exists for id: " + entrySet.getKey());
            }
        }

    }

    @Override
    public void beforeBulk(final long executionId, final BulkRequest request) {

        String serverId=APILocator.getServerAPI().readServerId();
        List<String> servers = Try.of(()->APILocator.getServerAPI().getReindexingServers()).getOrElse(List.of(APILocator.getServerAPI().readServerId()));
        Logger.info(this.getClass(), "-----------");
        Logger.info(this.getClass(), "Reindexing Server #  : " + (servers.indexOf(serverId)+1) + " of " + servers.size());
        Logger.info(this.getClass(), "Total Indexed        : " + contentletsIndexed);
        Logger.info(this.getClass(), "ReindexEntries found : " + totalWorkingRecords.get());
        Logger.info(this.getClass(), "BulkRequests created : " + request.numberOfActions());
        
        contentletsIndexed += request.numberOfActions();
        final Optional<String> duration = APILocator.getContentletIndexAPI().reindexTimeElapsed();
        duration.ifPresent(s -> Logger.info(this, "Full Reindex Elapsed : " + s));
        Logger.info(this.getClass(), "-----------");
    }

    @Override
    public void afterBulk(final long executionId, final BulkRequest request, final BulkResponse response) {
        Logger.debug(this.getClass(), "Bulk process completed");

        for (BulkItemResponse bulkItemResponse : response) {
            totalResponses.incrementAndGet();
            String id = getIdFromResponse(bulkItemResponse);

            ReindexEntry idx = workingRecords.remove(id);
            if (idx == null) continue;

            if (bulkItemResponse.isFailed() || bulkItemResponse.getResponse() == null) {
                addErrorToQueue(idx, bulkItemResponse.getFailure().getMessage());
            } else {
                addSuccessToQueue(idx);
            }
        }

        if (shouldRebuildBulkProcessor()) {
            ReindexThread.rebuildBulkIndexer();
        }
    }

    private String getIdFromResponse(BulkItemResponse bulkItemResponse) {
        String id;
        if (bulkItemResponse.isFailed() || bulkItemResponse.getResponse() == null) {
            id = getMatchingReservedIdIfAny(bulkItemResponse.getFailure().getId());
            if (id == null) {
                id = bulkItemResponse.getFailure().getId().split(StringPool.UNDERLINE)[0];
            }
        } else {
            id = getMatchingReservedIdIfAny(bulkItemResponse.getResponse().getId());
            if (id == null) {
                id = bulkItemResponse.getResponse().getId().split(StringPool.UNDERLINE)[0];
            }
        }
        return id;
    }

    private boolean shouldRebuildBulkProcessor() {
        return totalResponses.get() == 0 || ((double) successCount.get() / totalResponses.get() < 0.5);
    }

    private void addSuccessToQueue(ReindexEntry idx) {
        successCount.incrementAndGet();
        queue.add(new ReindexResult(idx));
    }

    private void addErrorToQueue(ReindexEntry idx, String errorMessage) {
        failureCount.incrementAndGet();
        queue.add(new ReindexResult(idx, errorMessage));
    }

    static String getMatchingReservedIdIfAny(String id) {
        String matchingReservedId = null;

        for (final String reservedId : RESERVED_IDS) {
            if(id.contains(reservedId)) {
                matchingReservedId = reservedId;
                break;
            }
        }

        return matchingReservedId;
    }

    @Override
    public void afterBulk(final long executionId, final BulkRequest request, final Throwable failure) {
        Logger.error(ReindexThread.class, "Bulk  process failed entirely:" + failure.getMessage(),
                failure);
        workingRecords.values().forEach(idx -> addErrorToQueue(idx, failure.getMessage()));
    }



    public static class ReindexResult {
        String error;
        ReindexEntry entry;
        boolean success;
        public ReindexResult(ReindexEntry entry, String error) {
            this.entry = entry;
            this.error = error;
            success = false;
        }
        public ReindexResult(ReindexEntry entry) {
            this.entry = entry;
            success = true;
        }
    }
}
