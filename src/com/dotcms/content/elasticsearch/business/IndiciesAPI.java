package com.dotcms.content.elasticsearch.business;

import java.sql.Connection;

import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotmarketing.exception.DotDataException;

/**
 * An API to store and retrieve information about current
 * Elastic Search Indicies
 * 
 * @author Jorge Urdaneta
 */
public interface IndiciesAPI {
    public static class IndiciesInfo {
        public String live, working, reindex_live, reindex_working, site_search;
    }
    
    /**
     * Returns IndiciesInfo instance with index names
     * stored.
     * 
     * @return IndiciesInfo instance
     */
    public IndiciesInfo loadIndicies() throws DotDataException;
    public IndiciesInfo loadIndicies(Connection conn) throws DotDataException;
    /**
     * Updates the información about ES indicies.
     * 
     * @param info
     */
    public void point(IndiciesInfo info) throws DotDataException;
    public void point(Connection conn,IndiciesInfo info) throws DotDataException;
    
}
