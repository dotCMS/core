package com.dotmarketing.util;

import com.dotcms.content.elasticsearch.util.BasicProcessStatus;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Jonathan Gamba
 * Date: 6/11/12
 * Time: 10:57 AM
 */
public class CleanAssetsThread extends Thread {

    private static CleanAssetsThread instance;
    private static BasicProcessStatus processStatus;

    /**
     * Return the current instance of this thread, this class is create it as a singleton a we need to recover this thread to track the process it is running
     *
     * @param restartIfDied creates or not a new instance depending of the current thread is alive or not
     * @return
     */
    public static CleanAssetsThread getInstance ( Boolean restartIfDied ) {

        if ( instance == null ) {

            instance = new CleanAssetsThread();
            processStatus = new BasicProcessStatus();
            processStatus.setStatusMessage( "0" );

        } else if ( !instance.isAlive() && restartIfDied ) {

            instance = new CleanAssetsThread();
            processStatus = new BasicProcessStatus();
            processStatus.setStatusMessage( "0" );
        }

        return instance;
    }

    private CleanAssetsThread () {
    }

    @Override
    public void run () {

        try {
            deleteAssetsWithNoInode();
        } catch ( DotDataException e ) {
            processStatus.setError( e.getMessage() );
            processStatus.stop();
            e.printStackTrace();
        }
    }

    /**
     * Deleting all inodes of the assets from inode table in case that inode in the table does not exist any more
     *
     * @return
     * @throws com.dotmarketing.exception.DotDataException
     *
     */
    @SuppressWarnings ("unchecked")
    synchronized int deleteAssetsWithNoInode () throws DotDataException {

        //Assest folder path
        String assetsPath = APILocator.getFileAPI().getRealAssetsRootPath();
        File assetsRootFolder = new File( assetsPath );

        //Reports path
        String reportsPath = "";
        if ( UtilMethods.isSet( Config.getStringProperty( "ASSET_REAL_PATH" ) ) ) {
            reportsPath = Config.getStringProperty( "ASSET_REAL_PATH" ) + File.separator + Config.getStringProperty( "REPORT_PATH" );
        } else {
            reportsPath = Config.CONTEXT.getRealPath( File.separator + Config.getStringProperty( "ASSET_PATH" ) + File.separator + Config.getStringProperty( "REPORT_PATH" ) );
        }
        File reportsFolder = new File( reportsPath );

        //Messages path
        String messagesPath = "";
        if ( UtilMethods.isSet( Config.getStringProperty( "ASSET_REAL_PATH" ) ) ) {
            messagesPath = Config.getStringProperty( "ASSET_REAL_PATH" ) + File.separator + "messages";
        } else {
            messagesPath = Config.CONTEXT.getRealPath( File.separator + Config.getStringProperty( "ASSET_PATH" ) + File.separator + "messages" );
        }
        File messagesFolder = new File( messagesPath );

        //Find the inodes for the assets files we need to keep
        DotConnect dc = new DotConnect();
        final String selectInodesSQL = "select i.inode from inode i where type = 'file_asset'";
        dc.setSQL( selectInodesSQL );
        List<HashMap<String, String>> results = dc.loadResults();
        List<String> fileAssetsInodes = new ArrayList<String>();
        for ( HashMap<String, String> r : results ) {
            fileAssetsInodes.add( r.get( "inode" ) );
        }

        //Find all the assets files candidates to deletion
        List<Object> filesAssetsCanBeParsed = new ArrayList<Object>();
        try {
            filesAssetsCanBeParsed = MaintenanceUtil.findFileAssetsCanBeParsed();
        } catch ( Exception ex ) {
            Logger.error( MaintenanceUtil.class, ex.getMessage(), ex );
        }
        List<String> fileAssetsListFromFileSystem = (List<String>) filesAssetsCanBeParsed.get( 0 );
        List<String> fileAssetsInodesListFromFileSystem = (List<String>) filesAssetsCanBeParsed.get( 1 );

        int counter = 0;
        for ( int i = 0; i < fileAssetsInodesListFromFileSystem.size(); i++ ) {

            if ( !fileAssetsInodes.contains( fileAssetsInodesListFromFileSystem.get( i ) ) ) {

                File assetFile = new File( fileAssetsListFromFileSystem.get( i ) );
                if ( !assetFile.getPath().startsWith( assetsRootFolder.getPath() + java.io.File.separator + "license" )
                        && !assetFile.getPath().startsWith( reportsFolder.getPath() )
                        && !assetFile.getPath().startsWith( messagesFolder.getPath() ) ) {

                    Logger.info( MaintenanceUtil.class, "Deleting " + assetFile.getPath() + "..." );

                    //And finally delete the old asset file
                    Boolean success = assetFile.delete();
                    if ( success ) {
                        counter++;
                        processStatus.setStatusMessage( String.valueOf( counter ) );
                    }
                }
            }
        }

        Logger.info( MaintenanceUtil.class, "Deleted " + counter + " files" );
        processStatus.stop();

        return counter;
    }

    public BasicProcessStatus getProcessStatus () {
        return processStatus;
    }

}