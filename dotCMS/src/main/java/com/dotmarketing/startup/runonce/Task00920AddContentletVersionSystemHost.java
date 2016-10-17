package com.dotmarketing.startup.runonce;

import com.dotmarketing.beans.Host;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

/**
 * User: Jonathan Gamba
 * Date: 6/6/12
 * Time: 10:07 AM
 */
public class Task00920AddContentletVersionSystemHost implements StartupTask {

    @Override
    public boolean forceRun () {

        return true;
    }

    @Override
    public void executeUpgrade () throws DotDataException, DotRuntimeException {

        //Update identifier table for the SYSTEM_HOST
        DotConnect dc = new DotConnect();
        dc.setSQL( "update identifier set asset_name = ? where id = ?" );
        dc.addParam( "system host" );
        dc.addParam( Host.SYSTEM_HOST );

        dc.loadResult();

        //Verify if we already have a contentlet version for the SYSTEM_HOST contentlet
        dc = new DotConnect();
        dc.setSQL( "select identifier from contentlet_version_info where identifier = ?" );
        dc.addParam( Host.SYSTEM_HOST );
        ArrayList<Map<String, String>> versionsResults = dc.loadResults();

        //Ok, we didn't found a version for this SYSTEM_HOST contentlet, so we need to create one
        if ( versionsResults == null || versionsResults.isEmpty() ) {

            //Getting the SYSTEM_HOST contentlet
            dc = new DotConnect();
            dc.setSQL( "select inode, language_id from contentlet where title = 'System Host'" );
            ArrayList<Map<String, String>> results = dc.loadResults();

            if ( results != null && results.size() > 0 ) {

                String inode = results.get( 0 ).get( "inode" );
                String languageId = results.get( 0 ).get( "language_id" );

                //Insert a contentlet version for the SYSTEM_HOST contentlet
                dc = new DotConnect();
                dc.setSQL( "insert into contentlet_version_info (identifier, lang, working_inode, live_inode, deleted, locked_by, locked_on, version_ts) values (?,?,?,?,?,?,?,?)" );
                dc.addParam( Host.SYSTEM_HOST );
                dc.addParam( Long.valueOf( languageId ) );
                dc.addParam( inode );
                dc.addParam( inode );
                if ( DbConnectionFactory.isPostgres() ) {
                    dc.addParam( false );
                } else if ( DbConnectionFactory.isMsSql() ) {
                    dc.addParam( 0 );
                } else if ( DbConnectionFactory.isMySql() ) {
                    dc.addParam( 0 );
                } else if ( DbConnectionFactory.isOracle() ) {
                    dc.addParam( 0 );
                }
                dc.addObject( null );
                dc.addParam( new Date() );
                dc.addParam( new Date() );

                dc.loadResult();
            } else {
                throw new DotRuntimeException( "Error querying SYSTEM_HOST contentlet." );
            }
        }

    }

}
