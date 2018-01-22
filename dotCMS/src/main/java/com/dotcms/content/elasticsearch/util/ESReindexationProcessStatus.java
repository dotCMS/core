package com.dotcms.content.elasticsearch.util;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Hashtable;
import java.util.Map;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.elasticsearch.business.ESContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;

public class ESReindexationProcessStatus implements Serializable {
    private static final ESContentletIndexAPI indexAPI=new ESContentletIndexAPI();


    public synchronized static boolean inFullReindexation () throws DotDataException {
        return inFullReindexation(DbConnectionFactory.getConnection());
    }
    
    public synchronized static boolean inFullReindexation (Connection conn) throws DotDataException {
        return indexAPI.isInFullReindex(conn);
    }

    @CloseDBIfOpened
    public synchronized static int getContentCountToIndex() throws DotDataException {

        DotConnect dc=new DotConnect();
        dc.setSQL("select count(*) as cc from contentlet_version_info");
        return Integer.parseInt(dc.loadObjectResults().get(0).get("cc").toString());
    }

    @CloseDBIfOpened
    public synchronized static int getLastIndexationProgress () throws DotDataException {
        long left = APILocator.getDistributedJournalAPI().recordsLeftToIndexForServer();
        int x = (int) (getContentCountToIndex()-left);

        return (x<0) ? 0 : x;
    }
    
    public synchronized static String currentIndexPath() throws DotDataException {
        IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();
        return "["+info.working+","+info.live+"]";
    }
    
    public synchronized static String getNewIndexPath() throws DotDataException {
        IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();
        return "["+info.reindex_working+","+info.reindex_live+"]";
    }

    @CloseDBIfOpened
    public synchronized static Map getProcessIndexationMap () throws DotDataException {
        Map<String, Object> theMap = new Hashtable<String, Object> ();

        theMap.put("inFullReindexation", inFullReindexation());
        // no reason to hit db if not needed
        if(inFullReindexation()){
            theMap.put("contentCountToIndex", getContentCountToIndex());
            theMap.put("lastIndexationProgress", getLastIndexationProgress());
            theMap.put("currentIndexPath", currentIndexPath());
            theMap.put("newIndexPath", getNewIndexPath());
        }
        return theMap;
    }
}
