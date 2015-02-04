package com.dotcms.autoupdater;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Jonathan Gamba
 *         Date: 2/4/15
 */
public class FileUtil {

    public static void copyDirectory ( String sourceDirName, String destinationDirName, boolean hardLinks ) {
        copyDirectory( new File( sourceDirName ), new File( destinationDirName ), hardLinks );
    }

    public static void copyDirectory ( File source, File destination, boolean hardLinks ) {
        copyDirectory( source, destination, hardLinks, null );
    }

    public static void copyDirectory ( File source, File destination ) {
        copyDirectory( source, destination, true );
    }

    public static void copyDirectory ( File source, File destination, boolean hardLinks, FileFilter filter ) {
        if ( source.exists() && source.isDirectory() ) {
            if ( !destination.exists() ) {
                destination.mkdirs();
            }

            File[] fileArray = filter != null ? source.listFiles( filter ) : source.listFiles();

            for ( int i = 0; i < fileArray.length; i++ ) {
                if ( fileArray[i].getName().endsWith( "xml" ) ) {
                    String name = fileArray[i].getName();
                    UpdateAgent.logger.info( "copy " + name );
                }

                if ( fileArray[i].isDirectory() ) {
                    copyDirectory( fileArray[i], new File( destination.getPath() + File.separator + fileArray[i].getName() ), hardLinks, filter );
                } else {
                    copyFile( fileArray[i], new File( destination.getPath() + File.separator + fileArray[i].getName() ), hardLinks );
                }
            }
        }
    }

    public static void copyFile ( File source, File destination, boolean hardLinks ) {

        if ( !source.exists() ) {
            return;
        }

        if ( (destination.getParentFile() != null) && (!destination.getParentFile().exists()) ) {
            destination.getParentFile().mkdirs();
        }

        if ( hardLinks ) {
            // I think we need to be sure to unlink first
            if ( destination.exists() ) {
                Path destinationPath = Paths.get( destination.getAbsolutePath() );
                try {
                    //"If the file is a symbolic link then the symbolic link itself, not the final target of the link, is deleted."
                    Files.delete( destinationPath );
                } catch ( IOException e ) {
                    UpdateAgent.logger.error( "Error removing hardLink: " + destination.getAbsolutePath(), e );
                }
            }

            try {

                Path newLink = Paths.get( destination.getAbsolutePath() );
                Path existingFile = Paths.get( source.getAbsolutePath() );

                Files.createLink( newLink, existingFile );
                // setting this means we will try again if we cannot hard link
                if ( !destination.exists() ) {
                    hardLinks = false;
                }
            } catch ( IOException e ) {
                UpdateAgent.logger.error( "Can't create hardLink. source: " + source.getAbsolutePath() + ", destination: " + destination.getAbsolutePath() );
                // setting this means we will try again if we cannot hard link
                hardLinks = false;
            }

        }
        if ( !hardLinks ) {

            FileChannel srcChannel = null;
            FileChannel dstChannel = null;

            try {
                srcChannel = new FileInputStream( source ).getChannel();
                dstChannel = new FileOutputStream( destination ).getChannel();

                dstChannel.transferFrom( srcChannel, 0, srcChannel.size() );
            } catch ( IOException ioe ) {
                UpdateAgent.logger.error( ioe.getMessage(), ioe );
            } finally {
                try {
                    if ( srcChannel != null ) {
                        srcChannel.close();
                    }
                    if ( dstChannel != null ) {
                        dstChannel.close();
                    }
                } catch ( IOException ioe ) {
                    UpdateAgent.logger.error( ioe.getMessage(), ioe );
                }
            }


        }

    }

}