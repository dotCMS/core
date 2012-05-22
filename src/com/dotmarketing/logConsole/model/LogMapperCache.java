package com.dotmarketing.logConsole.model;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.DotCacheException;

import java.util.Collection;

/**
 * Created by Jonathan Gamba.
 * Date: 5/21/12
 * Time: 4:25 PM
 */
public interface LogMapperCache extends Cachable {

    /**
     * Gets an LogMapperRow object from cache.
     *
     * @param logName
     * @return
     * @throws DotCacheException
     */
    public LogMapperRow get ( String logName ) throws DotCacheException;

    /**
     * Puts an LogMapperRow object in a cache.
     *
     * @param logMapperRow
     * @throws DotCacheException
     */
    public void put ( LogMapperRow logMapperRow ) throws DotCacheException;

    /**
     * Return all the records stored on cache for this primary group
     *
     * @return logMapperRows
     * @throws DotCacheException
     */
    public Collection<LogMapperRow> getAll () throws DotCacheException;

}