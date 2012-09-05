package com.dotmarketing.logConsole.model;

import java.util.List;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.logConsole.business.ConsoleLogFactory;
import com.dotmarketing.logConsole.business.ConsoleLogFactoryImpl;
import com.dotmarketing.util.Logger;

public class LogMapper {

    private static LogMapper instance = null;
    private List<LogMapperRow> logList = null;
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
    public List<LogMapperRow> getLogList () throws DotCacheException, DotDataException {

        //First lets try to find them in cache
        logList = cache.get();

        // Not in cache?
        if ( logList == null ) {

            //Lets hit the database...
            logList = consoleLogFactory.findLogMapper();

            //If we found something add them to cache
            if ( logList != null ) {
                cache.put( logList );
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

        List<LogMapperRow> logMapperRows = null;
        try {
            //Lets try to find all the logs...
            logMapperRows = getLogList();
        } catch ( Exception e ) {
            Logger.error( this, "isLogEnabled: Error retrieving LogMapperRows.", e );
        }

        if ( logMapperRows != null ) {

            for ( LogMapperRow mapperRow : logMapperRows ) {
                if ( logName.equalsIgnoreCase( mapperRow.getLog_name() ) && mapperRow.getEnabled() ) {
                    return true;
                }
            }
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
