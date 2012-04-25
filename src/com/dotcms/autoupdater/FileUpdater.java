package com.dotcms.autoupdater;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


/**
 * This class backups and updates the file system.
 *
 * @author andres
 */
public class FileUpdater {

    public static Logger logger;

    private File updateFile;
    private String home;
    private String backupFile;
    private Boolean dryrun;
    private List<String> delList;

    //We only delete files that are on one of these locations
    private String[] delLocations = { "src/", "dotCMS/html/", "WEB-INF/lib/" };

    public FileUpdater ( File updateFile, String home, String backupFile, Boolean dryrun ) {

        this.updateFile = updateFile;
        this.home = home;
        this.backupFile = backupFile;
        this.dryrun = dryrun;

        logger = UpdateAgent.logger;
    }

    /**
     * This method will be call it before the update process, actually this method will work as preparation for the update, basically what it does to to back-up the current .dotserver/ code
     *
     * @throws IOException
     * @throws UpdateException
     */
    public void preUpdate () throws IOException, UpdateException {

        logger.info( Messages.getString( "UpdateAgent.debug.start.validation" ) );

        //First if we don't have the ant jars, we extract them.  This is a pretty ugly hack, but there's no way to guarantee the user already has them
        File antLauncher = new File( home + File.separator + UpdateAgent.FOLDER_HOME_UPDATER + File.separator + "bin" + File.separator + "ant" + File.separator + "ant-launcher.jar" );
        if ( !antLauncher.exists() ) {
            logger.debug( Messages.getString( "UpdateAgent.debug.extracting.ant" ) );
            UpdateUtil.unzipDirectory( updateFile, "bin/ant", home + File.separator + UpdateAgent.FOLDER_HOME_UPDATER, false );
        }

        // Find if we have a build.xml in the update. If we do, we use that one.
        // Otherwise we use the current one
        File buildFile = new File( home + File.separator + UpdateAgent.FOLDER_HOME_UPDATER + File.separator + "build_new.xml" );
        boolean updateBuild = UpdateUtil.unzipFile( updateFile, "autoupdater/build.xml", buildFile );

        // Create the temp file structrue
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

        //First lets remove the "old" code in order to unzip the update on that folder .dotserver/
        File dotserverFolder = new File( home + File.separator + UpdateAgent.FOLDER_HOME_DOTSERVER );
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
        UpdateUtil.unzipDirectory( updateFile, UpdateAgent.FOLDER_HOME_DOTSERVER, home + File.separator + UpdateAgent.FOLDER_HOME_DOTSERVER, dryrun );

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

        if ( pp.postProcess( true ) ) {

            logger.info( Messages.getString( "UpdateAgent.debug.end.validation" ) );

            // At this point we should try to use the build file we got from the update zip
            File buildFile = new File( home + File.separator + UpdateAgent.FOLDER_HOME_UPDATER + File.separator + "build_new.xml" );

            //Create the ant invoker
            boolean success;
            AntInvoker invoker = new AntInvoker( home + File.separator + UpdateAgent.FOLDER_HOME_UPDATER );
            // Try to do a clean up.
            logger.debug( "Trying to clean update process traces..." );
            if ( buildFile.exists() ) {
                success = invoker.runTask( "clean", "build_new.xml" );
                buildFile.delete();
            } else {
                success = invoker.runTask( "clean", null );
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

        logger.info( Messages.getString( "UpdateAgent.debug.end.post.process" ) );
        return true;
    }

    private boolean verifyWritePermissions () throws UpdateException {

        List<String> missingPerms = new ArrayList<String>();
        for ( String fileName : delList ) {

            String fullFileName = home + File.separator + fileName;
            File file = new File( fullFileName );
            if ( file.exists() && !file.canWrite() ) {
                missingPerms.add( fileName );
            }
        }

        try {

            FileInputStream fis = new FileInputStream( updateFile );
            ZipInputStream zis = new ZipInputStream( new BufferedInputStream( fis ) );
            ZipEntry entry;

            while ( ( entry = zis.getNextEntry() ) != null ) {
                if ( !entry.isDirectory() ) {

                    File destFile = new File( home + File.separator + entry.getName() );
                    File parentFile = destFile.getParentFile();
                    if ( destFile.exists() && !destFile.canWrite() ) {
                        missingPerms.add( entry.getName() );
                        logger.debug( "Missing write permission on: " + entry.getName() );
                    } else {
                        //Recurse to see if the parent has write permissions
                        boolean done = false;
                        while ( !done ) {
                            if ( parentFile.exists() ) {
                                done = true;
                                if ( !parentFile.canWrite() ) {
                                    missingPerms.add( parentFile.getAbsolutePath() );
                                    logger.debug( "Missing write permission on: " + parentFile.getAbsolutePath() );
                                }
                            } else {
                                parentFile = parentFile.getParentFile();
                            }
                        }
                    }
                }
            }
        } catch ( FileNotFoundException e ) {
            logger.debug( "FileNotFoundException: " + e.getMessage(), e );
        } catch ( IOException e ) {
            logger.debug( "IOException: " + e.getMessage(), e );
        }

        if ( missingPerms.size() == 0 ) {
            return true;
        }
        StringBuilder sb = new StringBuilder();
        if ( missingPerms.size() == 1 ) {
            sb.append( Messages.getString( "FileUpdater.error.file.permission" ) );
        } else {
            sb.append( Messages.getString( "FileUpdater.error.files.permissions" ) );
        }

        if ( UpdateAgent.isDebug ) {
            throw new UpdateException( Messages.getString( "FileUpdater.debug.file.permission" ), UpdateException.ERROR );
        }

        if ( missingPerms.size() <= 10 ) {

            for ( String fileName : missingPerms ) {
                sb.append( fileName ).append( "\n" );
            }
        } else {
            if ( missingPerms.size() == 11 ) {
                for ( String fileName : missingPerms ) {
                    sb.append( fileName ).append( "\n" );
                }
            } else {
                for ( int i = 0; i < 10; i++ ) {
                    sb.append( missingPerms.get( i ) ).append( "\n" );
                }
                int left = missingPerms.size() - 10;
                sb.append( Messages.getString( "FileUpdater.text.other.files", left + "", UpdateAgent.logFile ) );
            }

        }

        throw new UpdateException( sb.toString(), UpdateException.ERROR );
    }

    /**
     * @throws IOException
     * @deprecated
     */
    public void getDelList () throws IOException {

        logger.debug( "Starting getDelList" );
        ZipFile zip = new ZipFile( updateFile );
        ZipEntry entry = zip.getEntry( "dellist" );

        delList = new ArrayList<String>();
        if ( entry != null ) {
            logger.debug( "dellist found" );
            InputStream is = zip.getInputStream( entry );
            InputStreamReader isr = new InputStreamReader( is );
            BufferedReader br = new BufferedReader( isr );
            String line;
            while ( ( line = br.readLine() ) != null ) {
                delList.add( line );
            }
        } else {
            logger.debug( "No dellist found" );
        }
        logger.debug( "Finished getDelList" );
    }

    private void doBackup ( boolean dryrun ) throws IOException {

        logger.info( Messages.getString( "FileUpdater.debug.start.backup" ) );
        ActivityIndicator.startIndicator();
        ZipOutputStream out = null;
        FileInputStream fis = new FileInputStream( updateFile );
        ZipInputStream zis = new ZipInputStream( new BufferedInputStream( fis ) );
        ZipEntry entry;
        boolean hasEntry = false;
        if ( !dryrun ) {
            out = new ZipOutputStream( new FileOutputStream( backupFile ) );
        }

        while ( ( entry = zis.getNextEntry() ) != null ) {
            if ( !entry.isDirectory() ) {
                if ( !entry.getName().equalsIgnoreCase( "dellist" ) ) {
                    File oldFile = new File( home + File.separator
                            + entry.getName() );
                    if ( oldFile.exists() && !oldFile.isDirectory() ) {
                        logger.debug( Messages.getString( "FileUpdater.debug.backup.file" ) + entry );
                        if ( !dryrun ) {
                            backup( out, oldFile, entry.getName() );

                        }
                        hasEntry = true;
                    }
                }

            }
        }

        ActivityIndicator.endIndicator();
        if ( hasEntry ) {
            if ( !dryrun ) {
                out.close();
            }
            logger.info( Messages.getString( "FileUpdater.debug.end.backup" ) );
        } else {
            logger.info( Messages.getString( "FileUpdater.text.no.backup" ) );
        }

    }

    /**
     * @param dryrun
     * @throws IOException
     * @deprecated
     */
    public void processData ( boolean dryrun ) throws IOException {

        File jardir = new File( home + File.separator + UpdateAgent.FOLDER_HOME_DOTSERVER + File.separator + "dotCMS" + File.separator + "WEB-INF/lib/" );

        logger.debug( "Looking for previous dotcms jars: " + jardir.getAbsolutePath() );

        Pattern pat = Pattern.compile( "dotcms_(\\d.\\d.*).jar" );
        for ( File jar : jardir.listFiles() ) {
            Matcher mat = pat.matcher( jar.getName() );
            if ( mat.matches() ) {
                logger.debug( "Found file: " + jar.getName() );
                if ( !dryrun ) {
                    logger.debug( "Deleting file: " + jar.getAbsolutePath() );
                    jar.delete();
                }
            }

        }

        if ( delList != null ) {
            logger.info( Messages.getString( "FileUpdater.debug.start.delete.files" ) );
            ActivityIndicator.startIndicator();
            for ( String fileName : delList ) {
                if ( isDeletable( fileName ) ) {
                    String fullFileName = home + File.separator + fileName;
                    logger.debug( "Looking for file: " + fullFileName );
                    File file = new File( fullFileName );
                    if ( file.exists() ) {

                        logger.debug( Messages.getString( "FileUpdater.debug.delete.file" ) + fullFileName );
                        if ( !dryrun ) {
                            file.delete();
                        }
                    }
                }

            }
            ActivityIndicator.endIndicator();
            logger.info( Messages.getString( "FileUpdater.debug.end.delete.files" ) );
        }

        logger.info( Messages.getString( "FileUpdater.debug.start.replace.files" ) );

        UpdateUtil.unzipDirectory( updateFile, null, home, dryrun );
        logger.info( Messages.getString( "FileUpdater.debug.end.replace.files" ) );


    }

    private boolean isDeletable ( String fileName ) {
        for ( String stub : delLocations ) {
            if ( fileName.startsWith( stub ) ) {
                return true;
            }
        }
        return false;
    }

    private void backup ( ZipOutputStream out, File f, String entry ) throws IOException {

        FileInputStream in = new FileInputStream( f );

        // Add ZIP entry to output stream.
        out.putNextEntry( new ZipEntry( entry ) );
        byte[] buf = new byte[1024];

        // Transfer bytes from the file to the ZIP file
        int len;
        while ( ( len = in.read( buf ) ) > 0 ) {
            out.write( buf, 0, len );
        }

        // Complete the entry
        out.closeEntry();
        in.close();

    }

}