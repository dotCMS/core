package com.dotcms.content.elasticsearch.business;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.FlushCacheRunnable;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;

import io.vavr.control.Try;

public class IndiciesFactoryImpl implements IndiciesFactory {

    protected static enum IndexTypes {
        WORKING, LIVE, REINDEX_WORKING, REINDEX_LIVE, SITE_SEARCH
    };

    protected static IndiciesCache cache = CacheLocator.getIndiciesCache();

    public IndiciesInfo loadIndicies() throws DotDataException {
        return loadIndicies(DbConnectionFactory.getConnection());
    }

    public IndiciesInfo loadIndicies(Connection conn) throws DotDataException {
        IndiciesInfo info = cache.get();
        if (info == null) {
            // build it once
            synchronized (this.getClass()) {
                if (conn == null) {
                    conn = DbConnectionFactory.getConnection();
                }
                info = cache.get();
                if (info == null) {
                    info = new IndiciesInfo();
                    DotConnect dc = new DotConnect();
                    dc.setSQL("SELECT index_name,index_type FROM indicies");
                    List<Map<String, Object>> results = dc.loadResults(conn);
                    for (Map<String, Object> rr : results) {
                        String name = (String) rr.get("index_name");
                        String type = (String) rr.get("index_type");
                        if (type.equalsIgnoreCase(IndexTypes.WORKING.toString()))
                            info.working = name;
                        else if (type.equalsIgnoreCase(IndexTypes.LIVE.toString()))
                            info.live = name;
                        else if (type.equalsIgnoreCase(IndexTypes.REINDEX_LIVE.toString()))
                            info.reindex_live = name;
                        else if (type.equalsIgnoreCase(IndexTypes.REINDEX_WORKING.toString()))
                            info.reindex_working = name;
                        else if (type.equalsIgnoreCase(IndexTypes.SITE_SEARCH.toString()))
                            info.site_search = name;

                    }
                    cache.put(info);
                }
            }
        }
        return info;
    }

    @Override
    public void point(IndiciesInfo newInfo) throws DotDataException {

        IndiciesInfo oldInfo = loadIndicies();
        DotConnect dc = new DotConnect();

        final String updateSQL = "UPDATE indicies set index_name=? where index_name=? and index_type=?";
        final String insertSQL = "INSERT INTO indicies VALUES(?,?)";
        final String deleteSQL = "DELETE from indicies where index_type=?";

        for (IndexTypes type : IndexTypes.values()) {
            final String indexType = type.toString().toLowerCase();
            final String oldValue = Try.of(() -> (String) IndiciesInfo.class.getDeclaredField(indexType).get(oldInfo)).getOrNull();
            final String newValue = Try.of(() -> (String) IndiciesInfo.class.getDeclaredField(indexType).get(newInfo)).getOrNull();

            if(newValue == null){
                dc.setSQL(deleteSQL).addParam(indexType).loadResult();
            }
            else if (oldValue == null) {
                dc.setSQL(deleteSQL).addParam(indexType).loadResult();
                dc.setSQL(insertSQL).addParam(newValue).addParam(indexType).loadResult();
            } else {
                dc.setSQL(updateSQL);
                dc.addParam(newValue);
                dc.addParam(oldValue);
                dc.addParam(indexType);
            }

        }
        
        cache.clearCache();

        HibernateUtil.addSyncCommitListener(new FlushCacheRunnable() {
            public void run() {
                cache.clearCache();
            }
        });
    }



}
