package com.dotmarketing.common.reindex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkResponse;

import com.dotmarketing.business.APILocator;

import com.dotmarketing.common.business.journal.IndexJournal;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;

class BulkActionListener implements ActionListener<BulkResponse> {

    BulkActionListener(final Map<String, IndexJournal> workingRecords) {
        this.workingRecords = workingRecords;
    }

    final Map<String, IndexJournal> workingRecords;

    private void handleSuccess(final List<IndexJournal> successful) {

        try {
            APILocator.getDistributedJournalAPI().deleteReindexEntry(successful);
        } catch (DotDataException e) {
            Logger.warnAndDebug(this.getClass(), "unable to delete indexjournal:" + e.getMessage(), e);
        }
    }

    private void handleFailure(final IndexJournal idx, String cause) {

        try {
            APILocator.getDistributedJournalAPI().markAsFailed(idx, cause);
        } catch (DotDataException e) {
            Logger.warnAndDebug(this.getClass(), "unable to reque indexjournal:" + idx, e);
        }
    }

    @Override
    public void onResponse(final BulkResponse bulkResponse) {

        List<IndexJournal> successful = new ArrayList<>(workingRecords.size());
        for (BulkItemResponse bulkItemResponse : bulkResponse) {
            DocWriteResponse itemResponse = bulkItemResponse.getResponse();
            String id = itemResponse.getId().substring(0, itemResponse.getId().lastIndexOf(StringPool.UNDERLINE));
            IndexJournal idx = workingRecords.get(id);
            if (idx == null)
                continue;
            if (bulkItemResponse.isFailed()) {
                handleFailure(idx, bulkItemResponse.getFailureMessage());
            } else {
                successful.add(idx);
            }
        }
        handleSuccess(successful);

    }

    @Override
    public void onFailure(final Exception ex) {

        Logger.error(ReindexThread.class, "Bulk  process failed entirely:" + ex.getMessage(), ex);
        workingRecords.values().forEach(idx -> handleFailure(idx, ex.getMessage()));

    }

}
