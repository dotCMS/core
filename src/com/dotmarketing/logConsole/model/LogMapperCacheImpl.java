package com.dotmarketing.logConsole.model;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Jonathan Gamba.
 * Date: 5/21/12
 * Time: 4:26 PM
 */
public class LogMapperCacheImpl implements LogMapperCache {

    private DotCacheAdministrator cache;

    protected final String primaryGroup = "LogMapperCache";
    protected final String[] groupNames = { primaryGroup };

    public LogMapperCacheImpl () {
        cache = CacheLocator.getCacheAdministrator();
    }

    /**
     * Gets an LogMapperRow object from cache.
     *
     * @param logName
     * @return
     * @throws DotCacheException
     */
    public LogMapperRow get ( String logName ) throws DotCacheException {
        return ( LogMapperRow ) cache.get( logName, primaryGroup );
    }

    /**
     * Puts an LogMapperRow object in a cache.
     *
     * @param logMapperRow
     * @throws DotCacheException
     */
    public void put ( LogMapperRow logMapperRow ) throws DotCacheException {
        cache.put( logMapperRow.getLog_name(), logMapperRow, primaryGroup );
    }

    /**
     * Return all the records stored on cache for this primary group
     *
     * @return logMapperRows
     * @throws DotCacheException
     */
    public Collection<LogMapperRow> getAll () throws DotCacheException {

        Collection<LogMapperRow> logMapperRows = null;

        //Get all the keys for this primary group
        Collection<String> keys = cache.getKeys( primaryGroup );
        if ( keys != null && !keys.isEmpty() ) {

            //Create an array with all the elements on cache for this primary group
            logMapperRows = new ArrayList<LogMapperRow>();

            for ( String key : keys ) {
                logMapperRows.add( get( key ) );
            }
        }

        return logMapperRows;
    }

    @Override
    public String getPrimaryGroup () {
        return primaryGroup;
    }

    @Override
    public String[] getGroups () {
        return groupNames;
    }

    @Override
    public void clearCache () {
        cache.flushGroup( primaryGroup );
    }

}