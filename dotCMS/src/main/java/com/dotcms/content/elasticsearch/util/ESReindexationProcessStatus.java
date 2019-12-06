package com.dotcms.content.elasticsearch.util;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImpl;
import com.dotcms.content.elasticsearch.business.IndiciesInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;

public class ESReindexationProcessStatus implements Serializable {
    private static final ContentletIndexAPIImpl indexAPI = new ContentletIndexAPIImpl();

    public synchronized static boolean inFullReindexation() throws DotDataException {
        return indexAPI.isInFullReindex();
    }

    @CloseDBIfOpened
    public synchronized static int getContentCountToIndex() throws DotDataException {

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
        IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
        return "[" + info.getWorking() + "," + info.getLive() + "]";
    }

    @CloseDBIfOpened
    public static String getNewIndexPath() throws DotDataException {
        IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
        return "[" + info.getReindexWorking() + "," + info.getReindexLive() + "]";
    }

    @CloseDBIfOpened
    public static Map getProcessIndexationMap() throws DotDataException {
        Map<String, Object> theMap = new Hashtable<String, Object>();

        theMap.put("inFullReindexation", inFullReindexation());
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
