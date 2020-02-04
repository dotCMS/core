package com.dotcms.content.elasticsearch.business;

import java.io.Serializable;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;


import com.dotmarketing.exception.DotDataException;

import io.vavr.control.Try;

/**
 * An API to store and retrieve information about current Elastic Search Indicies
 * 
 * @author Jorge Urdaneta
 */
public interface IndiciesAPI {

    /**
     * Returns IndiciesInfo instance with index names stored.
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
    public void point(IndiciesInfo newInfo) throws DotDataException;

}
