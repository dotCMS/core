package com.dotmarketing.common.reindex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteRequest.OpType;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;

class BulkActionListener implements ActionListener<BulkResponse> {

    BulkActionListener(final Map<String, ReindexEntry> workingRecords) {
        this.workingRecords = workingRecords;
    }

    final Map<String, ReindexEntry> workingRecords;

    
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

    @Override
    @CloseDBIfOpened
    public void onResponse(final BulkResponse bulkResponse) {

        List<ReindexEntry> successful = new ArrayList<>(workingRecords.size());
        for (BulkItemResponse bulkItemResponse : bulkResponse) {
            final DocWriteResponse itemResponse = bulkItemResponse.getResponse();
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
        
        /**
         * Delete query cache no matter what
         */
        CacheLocator.getESQueryCache().clearCache();

    }

    @Override
    public void onFailure(final Exception ex) {

        Logger.error(ReindexThread.class, "Bulk  process failed entirely:" + ex.getMessage(), ex);
        workingRecords.values().forEach(idx -> handleFailure(idx, ex.getMessage()));

    }

}
