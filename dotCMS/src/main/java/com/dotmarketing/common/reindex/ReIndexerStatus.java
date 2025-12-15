package com.dotmarketing.common.reindex;

import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImpl;
import com.dotcms.content.elasticsearch.business.IndicesInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;

public class ReIndexerStatus implements Serializable {
    private final ContentletIndexAPIImpl indexAPI;
    private final ReindexQueueAPI queueAPI;
    final ESIndexAPI esIndexAPI = APILocator.getESIndexAPI();

    private ReIndexerStatus(final ContentletIndexAPIImpl indexAPI, final ReindexQueueAPI queueAPI) {
        this.indexAPI = indexAPI;
        this.queueAPI = queueAPI;
    }


    private static class ReIndexerStatusHolder {
        static ReIndexerStatus statis = new ReIndexerStatus(new ContentletIndexAPIImpl(), APILocator.getReindexQueueAPI());

    }

    public ReIndexerStatus getInstance() {
        return ReIndexerStatusHolder.statis;

    }



    public boolean inFullReindexation() throws DotDataException {
        return indexAPI.isInFullReindex();
    }

    @CloseDBIfOpened
    public static int getContentCountToIndex() throws DotDataException {

        DotConnect dc = new DotConnect();
        dc.setSQL("select count(*) as cc from contentlet_version_info");
        return Integer.parseInt(dc.loadObjectResults().get(0).get("cc").toString());
    }

    @CloseDBIfOpened
    public int getLastIndexationProgress() throws DotDataException {
        return getLastIndexationProgress(getContentCountToIndex());
    }

    @CloseDBIfOpened
    public int getLastIndexationProgress(int countToIndex) throws DotDataException {
        long left = queueAPI.recordsInQueue();
        int x = (int) (countToIndex - left);

        return (x < 0) ? 0 : x;
    }

    @CloseDBIfOpened
    public String currentIndexPath() throws DotDataException {
        final IndicesInfo info = APILocator.getIndiciesAPI().loadLegacyIndices();
        return "[" + esIndexAPI.removeClusterIdFromName(info.getWorking()) + ","
                        + esIndexAPI.removeClusterIdFromName(info.getLive()) + "]";
    }

    @CloseDBIfOpened
    public String getNewIndexPath() throws DotDataException {
        final IndicesInfo info = APILocator.getIndiciesAPI().loadLegacyIndices();
        return "[" + esIndexAPI.removeClusterIdFromName(info.getReindexWorking()) + ","
                        + esIndexAPI.removeClusterIdFromName(info.getReindexLive()) + "]";
    }

    @CloseDBIfOpened
    public Map<String, Object> getProcessIndexationMap() throws DotDataException {
        Map<String, Object> theMap = new Hashtable<>();

        theMap.put("inFullReindexation", inFullReindexation());

        theMap.put("errorCount", queueAPI.failedRecordCount());



        // no reason to hit db if not needed
        if (inFullReindexation()) {
            final int countToIndex = getContentCountToIndex();
            theMap.put("contentCountToIndex", countToIndex);
            theMap.put("lastIndexationProgress", getLastIndexationProgress(countToIndex));
            theMap.put("currentIndexPath", currentIndexPath());
            theMap.put("newIndexPath", getNewIndexPath());
            theMap.put("reindexTimeElapsed", indexAPI.reindexTimeElapsed().orElse(null));
        }
        return theMap;
    }
}
