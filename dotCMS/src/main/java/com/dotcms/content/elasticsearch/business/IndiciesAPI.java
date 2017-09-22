package com.dotcms.content.elasticsearch.business;

import com.dotmarketing.exception.DotDataException;

import java.io.Serializable;
import java.sql.Connection;

/**
 * An API to store and retrieve information about current
 * Elastic Search Indicies
 * 
 * @author Jorge Urdaneta
 */
public interface IndiciesAPI {
    public static class IndiciesInfo implements Serializable {
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
