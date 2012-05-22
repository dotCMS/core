package com.dotmarketing.logConsole.model;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.logConsole.business.ConsoleLogFactory;
import com.dotmarketing.logConsole.business.ConsoleLogFactoryImpl;
import com.dotmarketing.util.Logger;

import java.util.Collection;

public class LogMapper {

    private static LogMapper instance = null;
    private Collection<LogMapperRow> logList = null;
    private LogMapperCache cache = CacheLocator.getLogMapperCache();
    private ConsoleLogFactory consoleLogFactory = new ConsoleLogFactoryImpl();

    public static LogMapper getInstance () {
        if ( instance == null ) {
            instance = new LogMapper();
        }
        return instance;
    }

    /**
     * Return the complete collection of logs mappers.
     * First we are going to try to find them on cache, if is not set in cache we will hit the database and then they are put in cache
     *
     * @return
     * @throws DotCacheException
     * @throws DotDataException
     */
    public Collection<LogMapperRow> getLogList () throws DotCacheException, DotDataException {

        //First lets try to find them in cache
        logList = cache.getAll();

        // Not in cache?
        if ( logList == null ) {

            //Lets hit the database...
            logList = consoleLogFactory.findLogMapper();

            //If we found something add them to cache
            if ( logList != null && !logList.isEmpty() ) {

                for ( LogMapperRow logMapperRow : logList ) {
                    cache.put( logMapperRow );
                }
            }
        }

        return logList;
    }

    /**
     * Verify if a log is enable, the filter is the log name
     *
     * @param logName
     * @return
     */
    public boolean isLogEnabled ( String logName ) {

        //First lets try to find it in cache
        LogMapperRow logMapperRow = null;
        try {
            logMapperRow = cache.get( logName );
        } catch ( DotCacheException e ) {
            Logger.warn( this, "isLogEnabled: Error retrieving LogMapperRow from cache.", e );
        }

        // Not in cache?
        if ( logMapperRow == null ) {

            //Lets try to find all the logs, if we find something it will be add it to cache
            Collection<LogMapperRow> logMapperRows = null;
            try {
                logMapperRows = getLogList();
            } catch ( Exception e ) {
                Logger.error( this, "isLogEnabled: Error retrieving LogMapperRows.", e );
            }

            if ( logMapperRows != null && !logMapperRows.isEmpty() ) {

                for ( LogMapperRow mapperRow : logMapperRows ) {
                    if ( logName.equals( mapperRow.getLog_name() ) && mapperRow.getEnabled() == 1 ) {
                        return true;
                    }
                }
            }
        } else {
            return logMapperRow.getEnabled() == 1;
        }

        return false;
    }

    /**
     * Will update what ever if have in memory, after this update the cache will be clean it.
     *
     * @throws DotDataException
     * @throws DotCacheException
     */
    public void updateLogsList () throws DotDataException, DotCacheException {

        synchronized ( this ) {

            for ( LogMapperRow logMapperRow : this.logList ) {

                try {
                    //Update the record
                    consoleLogFactory.updateLogMapper( logMapperRow );
                } catch ( DotDataException e ) {
                    Logger.error( this.getClass(), e.getMessage(), e );
                }
            }

            //We may had updates, lets clean the cache
            cache.clearCache();
        }

    }

}
