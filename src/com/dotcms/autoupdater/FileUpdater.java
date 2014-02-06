package com.dotcms.autoupdater;

import com.dotcms.repackage.tika_app_1_3.org.apache.log4j.Logger;
import com.liferay.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class backups and updates the file system.
 *
 * @author andres
 */
public class FileUpdater {

    public static Logger logger;

    private File updateFile;
    private String distributionHome;
    private String dotcmsHome;
    private Boolean dryrun;

    public FileUpdater ( File updateFile, String distributionHome, String dotcmsHome, String backupFile, Boolean dryrun ) {

        this.updateFile = updateFile;
        this.distributionHome = distributionHome;
        this.dotcmsHome = dotcmsHome;
        this.dryrun = dryrun;

        logger = UpdateAgent.logger;
    }

    /**
     * This method will be call it before the update process, actually this method will work as preparation for the update, basically what it does to to back-up the current .dotserver/ code
     *
     * @throws IOException
     * @throws UpdateException
     */
    public void preUpdate () throws UpdateException {

        logger.info( Messages.getString( "UpdateAgent.debug.start.backUp" ) );

        //Current format for the back-up folder
        SimpleDateFormat folderDateFormat = new SimpleDateFormat( "yyyyMMdd" );

        //This is the name of the folder where we are going to store the back-up
        String currentBackUpFolderName = folderDateFormat.format( new Date() );
        //Complete back-up path
        String backUpPath = distributionHome + File.separator + UpdateAgent.FOLDER_HOME_BACK_UP + File.separator + currentBackUpFolderName;
        //.dotserver folder path
        String dotserverPath = distributionHome + File.separator + UpdateAgent.FOLDER_HOME_DOTSERVER;
        //.bin folder path
        String binPath = distributionHome + File.separator + UpdateAgent.FOLDER_HOME_BIN;
        //.plugins folder path
        String pluginsPath = distributionHome + File.separator + UpdateAgent.FOLDER_HOME_PLUGINS;

        try {

            //First we need to create the back up for the current project, for this we need to user hard links, this back up could be huge
            FileUtil.copyDirectory( dotserverPath, backUpPath + File.separator + UpdateAgent.FOLDER_HOME_DOTSERVER, true );
            FileUtil.copyDirectory( pluginsPath, backUpPath + File.separator + UpdateAgent.FOLDER_HOME_PLUGINS, true );
            FileUtil.copyDirectory( binPath, backUpPath + File.separator + UpdateAgent.FOLDER_HOME_BIN, true );

        } catch ( Exception e ) {
            String error = Messages.getString( "UpdateAgent.error.ant.prepare.back-up" );
            if ( !UpdateAgent.isDebug ) {
                error += Messages.getString( "UpdateAgent.text.use.verbose", UpdateAgent.logFile );
            }
            logger.error( error, e );
            throw new UpdateException( error, UpdateException.ERROR );
        }
    }

    /**
     * Method that will handler the update process itself, unziping the download file on the .dotserver/ directory and aplying the required changes in there
     *
     * @return
     * @throws IOException
     * @throws UpdateException
     */
    public boolean doUpdate () throws IOException, UpdateException {

        //Current format for the back-up folder
        SimpleDateFormat folderDateFormat = new SimpleDateFormat( "yyyyMMdd" );

        //This is the name of the folder where we stored the back-up
        String currentBackUpFolderName = folderDateFormat.format( new Date() );
        //Complete back-up path
        String backUpPath = distributionHome + File.separator + UpdateAgent.FOLDER_HOME_BACK_UP + File.separator + currentBackUpFolderName;

        //.dotserver folder path
        String dotserverPath = distributionHome + File.separator + UpdateAgent.FOLDER_HOME_DOTSERVER;
        //.plugins folder path
        String pluginsPath = distributionHome + File.separator + UpdateAgent.FOLDER_HOME_PLUGINS;

        if ( !dryrun ) {

            //Apply the update on the distribution folders
            applyUpdateFor( UpdateAgent.FOLDER_HOME_DOTSERVER, null );//.dotserver
            applyUpdateFor( UpdateAgent.FOLDER_HOME_PLUGINS, null );//.plugins
            //The bin folder is a special case as it have files we can delete or even update, like the autoUpdater.sh or the build.conf
            applyUpdateFor( UpdateAgent.FOLDER_HOME_BIN, new String[]{"build.conf", "build.conf.bat", "autoUpdater.sh", "autoUpdater.bat"} );//.bin

            //++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //Now copy back the assets contents
            logger.debug( "Copying back backed assets folder..." );
            String assets = "assets";
            File assetsFolder = new File( backUpPath + File.separator + UpdateAgent.FOLDER_HOME_DOTSERVER + File.separator + assets );
            File destFolder = new File( dotserverPath + File.separator + assets );
            //Copying using hardlinks
            FileUtil.copyDirectory( assetsFolder, destFolder );

            //++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //Now copy back the dotsecure contents
            logger.debug( "Copying back backed dotsecure folder..." );
            String dotsecure = "dotsecure";
            File dotsecureFolder = new File( backUpPath + File.separator + UpdateAgent.FOLDER_HOME_DOTSERVER + File.separator + dotsecure );
            destFolder = new File( dotserverPath + File.separator + dotsecure );
            //Copying using hardlinks
            FileUtil.copyDirectory( dotsecureFolder, destFolder );

            //++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //Now copy back the plugins
            logger.debug( "Copying back backed plugins..." );
            File pluginsFolder = new File( backUpPath + File.separator + UpdateAgent.FOLDER_HOME_PLUGINS );
            destFolder = new File( pluginsPath );
            /*
             Copying using hardlinks, basically it will put back to the plugins folder the backed plugins.
             The copyDirectory method will NOT override any file, so updated files will be keep it.
             */
            FileUtil.copyDirectory( pluginsFolder, destFolder );
        }

        return true;
    }

    /**
     * Updates a given folder with the content of the update file
     *
     * @param forPath
     * @param exclusions list of files we don't want to remove from the given folder
     * @return
     * @throws IOException
     */
    private void applyUpdateFor ( String forPath, String[] exclusions ) throws UpdateException {

        Boolean success;
        try {
            //First lets remove the "old" code in order to unzip the update on that folder
            File updatedFolder = new File( distributionHome + File.separator + forPath );
            if ( exclusions != null ) {
                success = UpdateUtil.deleteDirectory( updatedFolder, exclusions );
            } else {
                success = UpdateUtil.deleteDirectory( updatedFolder );
            }
            if ( success ) {
                logger.debug( "Removed outdated folder: " + updatedFolder.getAbsolutePath() );
            } else {
                if ( exclusions == null ) {
                    logger.error( "Error removing outdated folder: " + updatedFolder.getAbsolutePath() );
                } else {
                    //If we have exclusions is normal to have a false success because the folder could not be removed as it have excluded files on it
                    logger.debug( "Removed outdated files in folder: " + updatedFolder.getAbsolutePath() );
                }
            }

            //Now we need to unzip the content of the update file into the given folder, we just removed it, so lets create it again....
            if ( !updatedFolder.exists() ) {
                success = updatedFolder.mkdirs();
                if ( success ) {
                    logger.debug( "Created folder: " + updatedFolder.getAbsolutePath() );
                } else {
                    logger.error( "Error creating folder: " + updatedFolder.getAbsolutePath() );
                }
            }
            success = UpdateUtil.unzipDirectory( updateFile, distributionHome, forPath, dryrun );
        } catch ( IOException e ) {
            String error = "Error unzipping update file on: " + forPath;
            if ( !UpdateAgent.isDebug ) {
                error += Messages.getString( "UpdateAgent.text.use.verbose", UpdateAgent.logFile );
            }
            throw new UpdateException( error, UpdateException.ERROR );
        }

        if ( !success ) {
            String error = "Error unzipping update file on: " + forPath;
            if ( !UpdateAgent.isDebug ) {
                error += Messages.getString( "UpdateAgent.text.use.verbose", UpdateAgent.logFile );
            }
            throw new UpdateException( error, UpdateException.ERROR );
        }
    }

    /**
     * The purpose of this method is to make final changes for the update processes, like cleaning, rebuilt fields, etc....
     *
     * @return
     * @throws UpdateException
     * @throws IOException
     */
    public Boolean postUpdate () throws UpdateException, IOException {

        logger.info( Messages.getString( "UpdateAgent.debug.start.post.process" ) );

        PostProcess postProcess = new PostProcess( distributionHome, dotcmsHome );

        if ( !dryrun ) {

            logger.info( Messages.getString( "UpdateAgent.debug.start.validation" ) );

            if ( postProcess.postProcess( true ) ) {

                logger.info( Messages.getString( "UpdateAgent.debug.end.validation" ) );

                //Boolean success = true;

                logger.debug( "Finished to clean update process traces." );

                /*if ( !success ) {
                    String error = Messages.getString( "UpdateAgent.error.ant.clean" );
                    if ( !UpdateAgent.isDebug ) {
                        error += Messages.getString( "UpdateAgent.text.use.verbose", UpdateAgent.logFile );
                    }
                    throw new UpdateException( error, UpdateException.ERROR );
                }*/
            } else {
                String error = Messages.getString( "UpdateAgent.error.plugin.incompatible" );
                if ( !UpdateAgent.isDebug ) {
                    error += Messages.getString( "UpdateAgent.text.use.verbose", UpdateAgent.logFile );
                }

                throw new UpdateException( error, UpdateException.ERROR );
            }
        }

        logger.info( Messages.getString( "UpdateAgent.debug.end.post.process" ) );
        return true;
    }

}