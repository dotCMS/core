package com.dotcms.content.elasticsearch.business;

import com.dotmarketing.exception.DotDataException;

/**
 * An API to store and retrieve information about current
 * Elastic Search Indicies
 * 
 * @author Jorge Urdaneta
 */
public interface IndiciesAPI {
    public static class IndiciesInfo {
        public String live, working, reindex_live, reindex_working;
    }
    
    /**
     * Returns IndiciesInfo instance with index names
     * stored.
     * 
     * @return IndiciesInfo instance
     */
    public IndiciesInfo loadIndicies() throws DotDataException;
    
    /**
     * Updates the información about ES indicies.
     * 
     * @param info
     */
    public void point(IndiciesInfo info) throws DotDataException;
}
