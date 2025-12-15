package com.dotcms.content.elasticsearch.util;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImpl;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.IndicesInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;

public class ESReindexationProcessStatus implements Serializable {
    private static final ContentletIndexAPIImpl indexAPI = new ContentletIndexAPIImpl();

    public static synchronized boolean inFullReindexation() throws DotDataException {
        return indexAPI.isInFullReindex();
    }

    @CloseDBIfOpened
    public static synchronized int getContentCountToIndex() throws DotDataException {

        DotConnect dc = new DotConnect();
        dc.setSQL("select count(*) as cc from contentlet_version_info");
        return Integer.parseInt(dc.loadObjectResults().get(0).get("cc").toString());
    }

    @CloseDBIfOpened
    public static int getLastIndexationProgress() throws DotDataException {
        return getLastIndexationProgress(getContentCountToIndex());
    }

    @CloseDBIfOpened
    public static int getLastIndexationProgress(int countToIndex) throws DotDataException {
        long left = APILocator.getReindexQueueAPI().recordsInQueue();
        int x = (int) (countToIndex - left);

        return (x < 0) ? 0 : x;
    }

    @CloseDBIfOpened
    public static String currentIndexPath() throws DotDataException {
        final IndicesInfo info = APILocator.getIndiciesAPI().loadLegacyIndices();
        final ESIndexAPI esIndexAPI = APILocator.getESIndexAPI();
        return "[" + esIndexAPI.removeClusterIdFromName(info.getWorking()) + "," + esIndexAPI
                .removeClusterIdFromName(info.getLive()) + "]";
    }

    @CloseDBIfOpened
    public static String getNewIndexPath() throws DotDataException {
        final IndicesInfo info = APILocator.getIndiciesAPI().loadLegacyIndices();
        final ESIndexAPI esIndexAPI = APILocator.getESIndexAPI();
        return "[" + esIndexAPI.removeClusterIdFromName(info.getReindexWorking()) + ","
                + esIndexAPI.removeClusterIdFromName(info.getReindexLive()) + "]";
    }

    @CloseDBIfOpened
    public static Map<String, Object> getProcessIndexationMap() throws DotDataException {
        Map<String, Object> theMap = new Hashtable<>();
        boolean inFullReindexation = inFullReindexation();
        theMap.put("inFullReindexation", inFullReindexation);

        theMap.put("errorCount", APILocator.getReindexQueueAPI().failedRecordCount());
        
        
        // no reason to hit db if not needed
        if (inFullReindexation) {
            final int countToIndex = getContentCountToIndex();
            final String timeElapsed = indexAPI.reindexTimeElapsed().orElse("n/a");
            theMap.put("contentCountToIndex", countToIndex);
            theMap.put("lastIndexationProgress", getLastIndexationProgress(countToIndex));
            theMap.put("currentIndexPath", currentIndexPath());
            theMap.put("newIndexPath", getNewIndexPath());
            theMap.put("reindexTimeElapsed",timeElapsed );
        }
        return theMap;
    }
}
