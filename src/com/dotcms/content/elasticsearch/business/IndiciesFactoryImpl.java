package com.dotcms.content.elasticsearch.business;

import java.util.List;
import java.util.Map;

import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;

public class IndiciesFactoryImpl implements IndiciesFactory {
    
    protected static enum IndexTypes {WORKING,LIVE,REINDEX_WORKING,REINDEX_LIVE,SITE_SEARCH};
    protected static IndiciesCache cache=CacheLocator.getIndiciesCache();
    
    public IndiciesInfo loadIndicies() throws DotDataException {
        IndiciesInfo info=cache.get();
        if(info==null) {
        	//build it once
        	synchronized (this.getClass()) {
        		if(cache.get() == null){
		            info=new IndiciesInfo();
		            DotConnect dc = new DotConnect();
		            dc.setSQL("SELECT index_name,index_type FROM indicies");
		            List<Map<String,Object>> results=dc.loadResults();
		            for(Map<String,Object> rr : results) {
		                String name=(String)rr.get("index_name");
		                String type=(String)rr.get("index_type");
		                if(type.equalsIgnoreCase(IndexTypes.WORKING.toString()))
		                    info.working=name;
		                else if(type.equalsIgnoreCase(IndexTypes.LIVE.toString()))
		                    info.live=name;
		                else if(type.equalsIgnoreCase(IndexTypes.REINDEX_LIVE.toString()))
		                    info.reindex_live=name;
		                else if(type.equalsIgnoreCase(IndexTypes.REINDEX_WORKING.toString()))
		                    info.reindex_working=name;
		                else if(type.equalsIgnoreCase(IndexTypes.SITE_SEARCH.toString()))
		                    info.site_search=name;
		                
		                
		            }
	            	cache.put(info);
				}
            }
        }
        return info;
    }
    
    public void point(IndiciesInfo info) throws DotDataException {
        DotConnect dc = new DotConnect();
        
        // first we delete them all
        dc.setSQL("DELETE FROM indicies");
        dc.loadResult();
        
        final String insertSQL="INSERT INTO indicies VALUES(?,?)";
        
        if(info.working!=null) {
            dc.setSQL(insertSQL);
            dc.addParam(info.working);
            dc.addParam(IndexTypes.WORKING.toString().toLowerCase());
            dc.loadResult();
        }
        
        if(info.live!=null) {
            dc.setSQL(insertSQL);
            dc.addParam(info.live);
            dc.addParam(IndexTypes.LIVE.toString().toLowerCase());
            dc.loadResult();
        }
        
        if(info.reindex_live!=null) {
            dc.setSQL(insertSQL);
            dc.addParam(info.reindex_live);
            dc.addParam(IndexTypes.REINDEX_LIVE.toString().toLowerCase());
            dc.loadResult();
        }
        
        if(info.reindex_working!=null) {
            dc.setSQL(insertSQL);
            dc.addParam(info.reindex_working);
            dc.addParam(IndexTypes.REINDEX_WORKING.toString().toLowerCase());
            dc.loadResult();
        }
        
        if(info.site_search!=null) {
            dc.setSQL(insertSQL);
            dc.addParam(info.site_search);
            dc.addParam(IndexTypes.SITE_SEARCH.toString().toLowerCase());
            dc.loadResult();
        }
        // we need to clear cache after commit. This way
        // is less error prone
        HibernateUtil.addCommitListener(new Runnable() {
            public void run() {
                cache.clearCache();
            }
        });
    }
}
