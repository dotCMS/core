package com.dotmarketing.common.reindex;


import com.google.common.collect.ImmutableList;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;

import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;

import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private long contentletsIndexed;

    BulkProcessorListener () {
        this.workingRecords = new HashMap<>();
    }

    public long getContentletsIndexed(){
        return contentletsIndexed;
    }

    @Override
    public void beforeBulk(final long executionId, final BulkRequest request) {
      
        String serverId=APILocator.getServerAPI().readServerId();
        List<String> servers = Try.of(()->APILocator.getServerAPI().getReindexingServers()).getOrElse(ImmutableList.of(APILocator.getServerAPI().readServerId()));
        Logger.info(this.getClass(), "-----------");
        Logger.info(this.getClass(), "Reindexing Server #  : " + (servers.indexOf(serverId)+1) + " of " + servers.size());
        Logger.info(this.getClass(), "Total Indexed        : " + contentletsIndexed);
        Logger.info(this.getClass(), "ReindexEntries found : " + workingRecords.size());
        Logger.info(this.getClass(), "BulkRequests created : " + request.numberOfActions());
        
        contentletsIndexed += request.numberOfActions();
        final Optional<String> duration = APILocator.getContentletIndexAPI().reindexTimeElapsed();
        if (duration.isPresent()) {
            Logger.info(this,        "Full Reindex Elapsed : " + duration.get() + "");
        }
        Logger.info(this.getClass(), "-----------");
    }

    @Override
    public void afterBulk(final long executionId, final BulkRequest request, final BulkResponse response) {
        final List<ReindexEntry> successful = new ArrayList<>();
        float totalResponses=0;
        for (BulkItemResponse bulkItemResponse : response) {
            DocWriteResponse itemResponse = bulkItemResponse.getResponse();
            totalResponses++;
            String id;
            if (bulkItemResponse.isFailed() || itemResponse == null) {
                id = bulkItemResponse.getFailure().getId().substring(0,
                        bulkItemResponse.getFailure().getId().lastIndexOf(StringPool.UNDERLINE));
            } else {
                id = itemResponse.getId()
                        .substring(0, itemResponse.getId().lastIndexOf(StringPool.UNDERLINE));
            }

            ReindexEntry idx = workingRecords.get(id);
            if (idx == null) {
                continue;
            }
            if (bulkItemResponse.isFailed() || itemResponse == null) {
                handleFailure(idx,
                        "bulk index failure:" + bulkItemResponse.getFailure().getMessage());
            } else {
                successful.add(idx);
            }
        }
        handleSuccess(successful);
        // 50% failure rate forces a rebuild of the BulkProcessor
        if(totalResponses==0 || (successful.size() / totalResponses < .5)) {
          ReindexThread.rebuildBulkIndexer();
        }
    }

    @Override
    public void afterBulk(final long executionId, final BulkRequest request, final Throwable failure) {
        Logger.error(ReindexThread.class, "Bulk  process failed entirely:" + failure.getMessage(),
                failure);
        workingRecords.values().forEach(idx -> handleFailure(idx, failure.getMessage()));
    }

    private void handleSuccess(final List<ReindexEntry> successful) {

        try {
            if (!successful.isEmpty()) {
                APILocator.getReindexQueueAPI().deleteReindexEntry(successful);
            }
        } catch (DotDataException e) {
            Logger.warnAndDebug(this.getClass(), "unable to delete indexjournal:" + e.getMessage(), e);
        }
    }

    private void handleFailure(final ReindexEntry idx, final String cause) {
        try {
            APILocator.getReindexQueueAPI().markAsFailed(idx, cause);
        } catch (DotDataException e) {
            Logger.warnAndDebug(this.getClass(), "unable to reque indexjournal:" + idx, e);
        }
    }
}
