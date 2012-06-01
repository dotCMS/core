package com.dotcms.autoupdater;

import com.liferay.util.FileUtil;
import org.apache.log4j.Logger;

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
    private String home;
    private Boolean dryrun;

    public FileUpdater ( File updateFile, String home, String backupFile, Boolean dryrun ) {

        this.updateFile = updateFile;
        this.home = home;
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
        String backUpPath = home + File.separator + UpdateAgent.FOLDER_HOME_BACK_UP + File.separator + currentBackUpFolderName;
        //.dotserver folder path
        String dotserverPath = home + File.separator + UpdateAgent.FOLDER_HOME_DOTSERVER;

        try {
            //First we need to create the back up for the current project, for this we need to user hard links, this back up could be huge
            FileUtil.copyDirectory(dotserverPath, dotserverPath, true);

            //First if we don't have the ant jars, we extract them.  This is a pretty ugly hack, but there's no way to guarantee the user already has them
            File antLauncher = new File( home + File.separator + UpdateAgent.FOLDER_HOME_UPDATER + File.separator + "bin" + File.separator + "ant" + File.separator + "ant-launcher.jar" );
            if ( !antLauncher.exists() ) {
                logger.debug( Messages.getString( "UpdateAgent.debug.extracting.ant" ) );
                UpdateUtil.unzipDirectory( updateFile, "bin/ant", home + File.separator + UpdateAgent.FOLDER_HOME_UPDATER, false );
            }
        } catch (Exception e) {
            String error = Messages.getString( "UpdateAgent.error.ant.prepare.back-up" );
            if ( !UpdateAgent.isDebug ) {
                error += Messages.getString( "UpdateAgent.text.use.verbose", UpdateAgent.logFile );
            }
            logger.error( error, e );
            throw new UpdateException( error, UpdateException.ERROR );
        }

        /*// Find if we have a build.xml in the update. If we do, we use that one.
        // Otherwise we use the current one
        File buildFile = new File( home + File.separator + UpdateAgent.FOLDER_HOME_UPDATER + File.separator + "build_new.xml" );
        boolean updateBuild = UpdateUtil.unzipFile( updateFile, "autoupdater/build.xml", buildFile );

        // Create the back up folder using ant tasks
        AntInvoker invoker = new AntInvoker( home + File.separator + UpdateAgent.FOLDER_HOME_UPDATER );
        boolean ret;
        if ( updateBuild ) {
            ret = invoker.runTask( "prepare.back-up", "build_new.xml" );
        } else {
            ret = invoker.runTask( "prepare.back-up", null );
        }

        if ( !ret ) {
            String error = Messages.getString( "UpdateAgent.error.ant.prepare.back-up" );
            if ( !UpdateAgent.isDebug ) {
                error += Messages.getString( "UpdateAgent.text.use.verbose", UpdateAgent.logFile );
            }
            throw new UpdateException( error, UpdateException.ERROR );
        }*/
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
        String backUpPath = home + File.separator + UpdateAgent.FOLDER_HOME_BACK_UP + File.separator + currentBackUpFolderName;
        //.dotserver folder path
        String dotserverPath = home + File.separator + UpdateAgent.FOLDER_HOME_DOTSERVER;

        if ( !dryrun ) {

            //First lets remove the "old" code in order to unzip the update on that folder .dotserver/
            File dotserverFolder = new File( dotserverPath );
            Boolean success = UpdateUtil.deleteDirectory( dotserverFolder );
            if ( success ) {
                logger.debug( "Removed outdated folder: " + dotserverFolder.getAbsolutePath() );
            } else {
                logger.error( "Error removing outdated folder: " + dotserverFolder.getAbsolutePath() );
            }

            //Now we need to unzip the content of the update file into the .dotserver folder, we just removed it, so lets create it again....
            if ( !dotserverFolder.exists() ) {
                success = dotserverFolder.mkdirs();
                if ( success ) {
                    logger.debug( "Created folder: " + dotserverFolder.getAbsolutePath() );
                } else {
                    logger.error( "Error creating folder: " + dotserverFolder.getAbsolutePath() );
                }
            }
            success = UpdateUtil.unzipDirectory( updateFile, UpdateAgent.FOLDER_HOME_DOTSERVER, dotserverPath, dryrun );

            if ( success ) {
                //++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                //Now lets copy the assets contents

                //Assets folder
                logger.debug( "Copying assets folder..." );
                String assets = "dotCMS" + File.separator + "assets";
                File assetsFolder = new File( backUpPath + File.separator + assets );
                File destFolder = new File( dotserverFolder + File.separator + assets );
                //Copying using hardlinks
                FileUtil.copyDirectory(assetsFolder, destFolder, true);
                //copyFolder( assetsFolder, destFolder );

                //++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                //Now lets copy the esdata contents

                //esdata folder
                logger.debug( "Copying esdata folder..." );
                String esdata = "dotCMS" + File.separator + "dotsecure" + File.separator + "esdata";
                File esdataFolder = new File( backUpPath + File.separator + esdata );
                destFolder = new File( dotserverFolder + File.separator + esdata );
                //Copying using hardlinks
                FileUtil.copyDirectory(esdataFolder, destFolder, true);
                //copyFolder( esdataFolder, destFolder );

                //++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                //Now lets copy the plugins contents

                //plugins folder
                logger.debug( "Copying plugins folder..." );
                String plugins = "plugins";
                String pluginsTemp = "plugins_temp";

                destFolder = new File( dotserverFolder + File.separator + plugins );
                File tempDestFolder = new File( dotserverFolder + File.separator + pluginsTemp );

                //First we need to move this folder to a temporal one in order to extract some files later
                if (destFolder.exists()) {
                    success = destFolder.renameTo( tempDestFolder );

                    if ( !success ) {
                        String error = "Error updating plugins...";
                        if ( !UpdateAgent.isDebug ) {
                            error += Messages.getString( "UpdateAgent.text.use.verbose", UpdateAgent.logFile );
                        }
                        throw new UpdateException( error, UpdateException.ERROR );
                    }
                }

                //Now we need to copy the back up plugins folder
                File pluginsFolder = new File( backUpPath + File.separator + plugins );
                //Copying using hardlinks
                FileUtil.copyDirectory(pluginsFolder, destFolder, true);
                //copyFolder( pluginsFolder, destFolder );

                //Now we need to remove the common.xml and plugins.xml
                File commonXML = new File( dotserverFolder + File.separator + plugins + File.separator + "common.xml" );
                File pluginsXML = new File( dotserverFolder + File.separator + plugins + File.separator + "plugins.xml" );
                if (commonXML.exists()) {
                    commonXML.delete();
                }
                if (pluginsXML.exists()) {
                    pluginsXML.delete();
                }
                //And copying them from the temporal plugins folder
                File origCommonXML = new File( dotserverFolder + File.separator + pluginsTemp + File.separator + "common.xml" );
                File origPluginsXML = new File( dotserverFolder + File.separator + pluginsTemp + File.separator + "plugins.xml" );
                if (origCommonXML.exists()) {
                    origCommonXML.renameTo( commonXML );
                }
                if (origPluginsXML.exists()) {
                    origPluginsXML.renameTo( pluginsXML );
                }

                //And finally lets remove the temporal updated plugins folder
                UpdateUtil.deleteDirectory( tempDestFolder );

            } else {
                String error = "Error unzipping update file on: " + dotserverPath;
                if ( !UpdateAgent.isDebug ) {
                    error += Messages.getString( "UpdateAgent.text.use.verbose", UpdateAgent.logFile );
                }

                throw new UpdateException( error, UpdateException.ERROR );
            }
        }

        return true;
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

        PostProcess pp = new PostProcess();
        pp.setHome( home + File.separator + UpdateAgent.FOLDER_HOME_DOTSERVER );

        if ( !dryrun ) {

            logger.info( Messages.getString( "UpdateAgent.debug.start.validation" ) );

            if ( pp.postProcess( true ) ) {

                logger.info( Messages.getString( "UpdateAgent.debug.end.validation" ) );

                // At this point we should try to use the build file we got from the update zip
                File buildFile = new File( home + File.separator + UpdateAgent.FOLDER_HOME_UPDATER + File.separator + "build_new.xml" );

                Boolean success = true;

                //Deleting if exist the extracted build file
                if ( buildFile.exists() ) {
                    success = buildFile.delete();
                }

                //Deleting manually the bin folder on the autoupdater directory
                File binFolder = new File( home + File.separator + UpdateAgent.FOLDER_HOME_UPDATER + File.separator + "bin" );
                if (binFolder.exists()) {
                    success = UpdateUtil.deleteDirectory( binFolder );
                }

                logger.debug( "Finished to clean update process traces." );

                if ( !success ) {
                    String error = Messages.getString( "UpdateAgent.error.ant.clean" );
                    if ( !UpdateAgent.isDebug ) {
                        error += Messages.getString( "UpdateAgent.text.use.verbose", UpdateAgent.logFile );
                    }
                    throw new UpdateException( error, UpdateException.ERROR );
                }
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

    /**
     * Copy the contents of a given folder into another
     *
     * @param srcFolder
     * @param destFolder
     * @throws UpdateException
     * @throws IOException
     */
    private void copyFolder ( File srcFolder, File destFolder ) throws UpdateException, IOException {

        if ( srcFolder.exists() ) {//It may not exists, if don't exist is ok.....

            //Where we are going to copy the assets contents
            if ( !destFolder.exists() ) {
                Boolean success = destFolder.mkdirs();
                if ( success ) {
                    logger.debug( "Created folder: " + destFolder );
                } else {
                    String error = "Error creating folder: " + destFolder;
                    if ( !UpdateAgent.isDebug ) {
                        error += Messages.getString( "UpdateAgent.text.use.verbose", UpdateAgent.logFile );
                    }

                    throw new UpdateException( error, UpdateException.ERROR );
                }
            }

            //And finally copy the contents
            UpdateUtil.copyFolder( srcFolder, destFolder );
        }
    }

}