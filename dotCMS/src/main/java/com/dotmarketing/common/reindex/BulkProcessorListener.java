package com.dotmarketing.common.reindex;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;
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
    public void beforeBulk(long executionId, BulkRequest request) {
        Logger.info(this.getClass(), "-----------");
        Logger.info(this.getClass(), "Total Indexed :" + contentletsIndexed);
        Logger.info(this.getClass(), "ReindexEntries found : " + workingRecords.size());
        Logger.info(this.getClass(), "BulkRequests created : " + request.numberOfActions());
        contentletsIndexed += request.numberOfActions();
        Optional<String> duration = APILocator.getContentletIndexAPI().reindexTimeElapsed();
        if (duration.isPresent()) {
            Logger.info(this, "Full Reindex Elapsed : " + duration.get() + "");
        }
        Logger.info(this.getClass(), "-----------");
    }

    @Override
    public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
        List<ReindexEntry> successful = new ArrayList<>();
        for (BulkItemResponse bulkItemResponse : response) {
            DocWriteResponse itemResponse = bulkItemResponse.getResponse();


            String id = itemResponse.getId().substring(0, itemResponse.getId().lastIndexOf(
                    StringPool.UNDERLINE));
            ReindexEntry idx = workingRecords.remove(id);
            if (idx == null)
                continue;
            if (bulkItemResponse.isFailed()) {
                handleFailure(idx, "bulk index failure:" + bulkItemResponse.getFailure().getMessage());
            } else {
                successful.add(idx);
            }
        }
        handleSuccess(successful);
    }

    @Override
    public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
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

    private void handleFailure(final ReindexEntry idx, String cause) {
        try {
            APILocator.getReindexQueueAPI().markAsFailed(idx, cause);
        } catch (DotDataException e) {
            Logger.warnAndDebug(this.getClass(), "unable to reque indexjournal:" + idx, e);
        }
    }
}
