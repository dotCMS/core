package com.dotcms.content.elasticsearch.util;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;

import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;

public class ESReindexationProcessStatus implements Serializable {
    private static final ESIndexAPI indexAPI=new ESIndexAPI();
    
    public synchronized static boolean inFullReindexation () throws DotDataException {
        return indexAPI.isInFullReindex();
    }
    
    public synchronized static int getContentCountToIndex() throws DotDataException {
        try {
            DotConnect dc=new DotConnect();
            dc.setSQL("select count(*) as cc from contentlet_lang_version_info");
            return Integer.parseInt(dc.loadObjectResults().get(0).get("cc").toString());
        }
        finally {
            HibernateUtil.closeSession();
        }
    }
    
    public synchronized static int getLastIndexationProgress () throws DotDataException {
        try {
            long left = APILocator.getDistributedJournalAPI().recordsLeftToIndexForServer();
            return (int) (getContentCountToIndex()-left);
        }
        finally {
            HibernateUtil.closeSession();
        }
        
    }
    
    public synchronized static String currentIndexPath() throws DotDataException {
        IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();
        return "["+info.working+","+info.live+"]";
    }
    
    public synchronized static String getNewIndexPath() throws DotDataException {
        IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();
        return "["+info.reindex_working+","+info.reindex_live+"]";
    }
    
    public synchronized static Map getProcessIndexationMap () throws DotDataException {
        try {
            Map<String, Object> theMap = new Hashtable<String, Object> ();
            theMap.put("inFullReindexation", inFullReindexation());
            theMap.put("contentCountToIndex", getContentCountToIndex());
            theMap.put("lastIndexationProgress", getLastIndexationProgress());
            theMap.put("currentIndexPath", currentIndexPath());
            theMap.put("newIndexPath", getNewIndexPath());
            return theMap;
        }
        finally {
            HibernateUtil.closeSession();
        }
    }
}
