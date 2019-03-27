package com.dotcms.content.elasticsearch.business;

import java.sql.Connection;

import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotmarketing.exception.DotDataException;

/**
 * Interface for the Factory that actually stores ES 
 * index name data.
 * 
 * @author Jorge Urdaneta
 *
 */
public interface IndiciesFactory {
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
