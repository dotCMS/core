package com.dotcms.autoupdater;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import java.io.*;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;


public class UpdateUtil {

    /**
     * This method will unzip a given file name from a given zip file
     *
     * @param zipFile
     * @param fileName
     * @param destFile
     * @return
     * @throws IOException
     */
    public static boolean unzipFile ( File zipFile, String fileName, File destFile ) throws IOException {

        ZipFile zip = new ZipFile( zipFile );
        ZipEntry entry = zip.getEntry( fileName );
        if ( entry == null ) {
            return false;
        }
        InputStream is = zip.getInputStream( entry );
        int BUFFER = 1024;
        int count;
        byte data[] = new byte[BUFFER];

        File parentFile = destFile.getParentFile();
        if ( !parentFile.exists() ) {
            parentFile.mkdirs();
        }
        FileOutputStream fos = new FileOutputStream( destFile );
        BufferedOutputStream dest = new BufferedOutputStream( fos, BUFFER );
        while ( ( count = is.read( data, 0, BUFFER ) ) != -1 ) {
            dest.write( data, 0, count );
        }
        dest.flush();
        dest.close();

        return true;
    }

    /**
     * This method will unzip a given zip file into a given direcoty
     *
     * @param zipFile
     * @param directoryName
     * @param home
     * @param dryrun
     * @return
     * @throws IOException
     */
    public static boolean unzipDirectory ( File zipFile, String directoryName, String home, boolean dryrun ) throws IOException {

        ActivityIndicator.startIndicator();
        FileInputStream fis = new FileInputStream( zipFile );
        ZipInputStream zis = new ZipInputStream( new BufferedInputStream( fis ) );

        ZipEntry entry;
        BufferedOutputStream dest;
        int BUFFER = 1024;
        while ( ( entry = zis.getNextEntry() ) != null ) {
            if ( !entry.isDirectory() ) {
                if ( directoryName == null || entry.getName().contains( directoryName ) ) {

                    UpdateAgent.logger.debug( Messages.getString( "FileUpdater.debug.extract.file" ) + entry );

                    if ( !dryrun ) {

                        int count;
                        byte data[] = new byte[BUFFER];

                        /***************************************************************
                         * ZIP ENTRY FILES MUST HAVE '/' AS SEPARATOR ON ANY PLATFORM *
                         ***************************************************************/

                        String entryName = entry.getName().replace( UpdateAgent.FOLDER_HOME_DOTSERVER + '/', "" );
                        File destFile = new File( home + File.separator + entryName );
                        UpdateAgent.logger.debug( destFile.getAbsoluteFile() );

                        File parentFile = destFile.getParentFile();
                        if ( !parentFile.exists() ) {
                            parentFile.mkdirs();
                        }

                        FileOutputStream fos = new FileOutputStream( destFile );
                        dest = new BufferedOutputStream( fos, BUFFER );
                        while ( ( count = zis.read( data, 0, BUFFER ) ) != -1 ) {
                            dest.write( data, 0, count );
                        }
                        dest.flush();
                        dest.close();
                    }

                }
            } else {

                //This code is to be certain that empty files are going to be create them as well....
                if ( directoryName == null || entry.getName().contains( directoryName ) ) {

                    String entryName = entry.getName().replace( UpdateAgent.FOLDER_HOME_DOTSERVER + '/', "" );
                    File destFile = new File( home + File.separator + entryName );

                    if (!destFile.exists()) {

                        destFile.mkdirs();
                        UpdateAgent.logger.debug( destFile.getAbsoluteFile() );
                    }

                }
            }
        }
        ActivityIndicator.endIndicator();

        return true;
    }

    public static Properties getInnerFileProps ( File zipFile ) throws IOException, UpdateException {

        // Lets look for the dotCMS jar inside the zip file
        FileInputStream fis = new FileInputStream( zipFile );
        ZipInputStream zis = new ZipInputStream( new BufferedInputStream( fis ) );
        ZipEntry entry;

        /***************************************************************
         * ZIP ENTRY FILES MUST HAVE '/' AS SEPARATOR ON ANY PLATFORM *
         ***************************************************************/

        while ( ( entry = zis.getNextEntry() ) != null ) {
            String entryName1 = entry.getName();
            if ( entryName1.startsWith( "dotserver/dotCMS/WEB-INF/lib/dotcms_" ) && !entryName1.startsWith( "dotserver/dotCMS/WEB-INF/lib/dotcms_ant" ) ) {

                // We found it
                ZipInputStream zis2 = new ZipInputStream( new BufferedInputStream( zis ) );
                ZipEntry entry2;
                while ( ( entry2 = zis2.getNextEntry() ) != null ) {
                    String entryName2 = entry2.getName();
                    // Let's look inside to see if we find it
                    if ( entryName2.equals( "com/liferay/portal/util/build.properties" ) ) {
                        Properties props = new Properties();
                        props.load( zis2 );
                        return props;
                    }
                }
            }
        }

        throw new UpdateException( Messages.getString( "UpdateUtil.error.no.version" ), UpdateException.ERROR );
    }

    public static String getFileMinorVersion ( File zipFile ) throws IOException, UpdateException {

        Properties props = getInnerFileProps( zipFile );
        return getBuildVersion( props );
    }

    public static String getBuildVersion ( Properties props ) {

        String minor = props.getProperty( "dotcms.release.build" );

        if ( minor != null && minor.equals( "" ) ) {
            minor = null;
        }

        return minor;
    }

    public static String getFileMayorVersion ( File zipFile ) throws IOException, UpdateException {

        Properties props = getInnerFileProps( zipFile );
        return props.getProperty( "dotcms.release.version" );
    }

    public static String getMD5 ( File f ) throws IOException {

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance( "MD5" );
            InputStream is = new FileInputStream( f );
            byte[] buffer = new byte[8192];
            int read;
            while ( ( read = is.read( buffer ) ) > 0 ) {
                digest.update( buffer, 0, read );
            }

        } catch ( NoSuchAlgorithmException e ) {
            UpdateAgent.logger.debug( "NoSuchAlgorithmException: " + e.getMessage(), e );
        }

        byte[] md5sum = digest.digest();
        BigInteger bigInt = new BigInteger( 1, md5sum );

        return bigInt.toString( 16 );
    }

    public static boolean confirmUI ( String confirmText ) {

        boolean done = false;
        boolean ret = false;

        while ( !done ) {
            System.out.println( confirmText );
            InputStreamReader isr = new InputStreamReader( System.in );
            BufferedReader br = new BufferedReader( isr );
            try {
                String sample = br.readLine();
                if ( sample.equalsIgnoreCase( Messages.getString( "UpdateAgent.text.yes" ) ) ) {
                    done = true;
                    ret = true;
                }
                if ( sample.equalsIgnoreCase( Messages.getString( "UpdateAgent.text.y" ) ) ) {
                    System.out.println( Messages.getString( "UpdateAgent.text.yes.or.no" ) );
                }
                if ( sample.equalsIgnoreCase( Messages.getString( "UpdateAgent.text.no" ) ) || sample.equalsIgnoreCase( Messages.getString( "UpdateAgent.text.n" ) ) ) {
                    done = true;
                }

            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public static void confirm ( CommandLine line, String confirmText ) throws UpdateException {

        // Ask for confirmation
        boolean doUpgrade;
        if ( line.hasOption( UpdateOptions.FORCE ) || line.hasOption( UpdateOptions.DRY_RUN ) ) {
            doUpgrade = true;
        } else {
            doUpgrade = UpdateUtil.confirmUI( confirmText );
        }

        if ( !doUpgrade ) {
            throw new UpdateException( Messages.getString( "UpdateAgent.cancel.user" ), UpdateException.CANCEL );
        }
    }

    /**
     * Will return a property value of a given property for the autoupdater.jar MANIFEST.MF file
     *
     * @param property
     * @return
     */
    public static String getManifestValue ( String property ) {

        Class clazz = UpdateAgent.class;

        String className = clazz.getSimpleName();
        String classFileName = className + ".class";
        String pathToThisClass = clazz.getResource( classFileName ).toString();

        int mark = pathToThisClass.indexOf( "!" );

        try {
            String pathToManifest = pathToThisClass.substring( 0, mark + 1 );

            pathToManifest += "/META-INF/MANIFEST.MF";
            Manifest manifest = new Manifest( new URL( pathToManifest ).openStream() );

            return manifest.getMainAttributes().getValue( property );
        } catch ( MalformedURLException e ) {
            UpdateAgent.logger.error( Messages.getString( "UpdateAgent.error.get.autoupdater.jar.version" ) + e.getMessage() );
            UpdateAgent.logger.debug( "MalformedURLException: ", e );
        } catch ( IOException e ) {
            UpdateAgent.logger.error( Messages.getString( "UpdateAgent.error.get.autoupdater.jar.version" ) + e.getMessage() );
            UpdateAgent.logger.debug( "IOException: ", e );
        }
        return Messages.getString( "UpdateAgent.text.unknown" );
    }

    public static void printHelp ( Options options, String helpText ) {

        HelpFormatter formatter = new HelpFormatter();
        String txt = Messages.getString( "UpdateAgent.text.agent.version" ) + getManifestValue( UpdateAgent.MANIFEST_PROPERTY_AGENT_VERSION ) + "\n";
        txt += helpText;
        formatter.printHelp( "autoUpdater <options>", "", options, txt );
    }

    /**
     * This method will delete a given folder, it will make it recursively removing from the children to the parent
     *
     * @param dir
     * @return
     */
    public static boolean deleteDirectory ( File dir ) {

        if ( dir.isDirectory() ) {
            String[] children = dir.list();
            for ( String child : children ) {
                boolean success = deleteDirectory( new File( dir, child ) );
                if ( !success ) {
                    return false;
                }
            }
        }

        //The directory is now empty so delete it
        return dir.delete();
    }

    /**
     * Copy the contents of a given folder into another
     *
     * @param src
     * @param dest
     * @throws IOException
     */
    public static void copyFolder ( File src, File dest ) throws IOException {

        if ( src.isDirectory() ) {

            //if directory not exists, create it
            if ( !dest.exists() ) {
                dest.mkdir();
                UpdateAgent.logger.debug( "Directory copied from " + src + "  to " + dest );
            }

            //list all the directory contents
            String files[] = src.list();

            for ( String file : files ) {

                //construct the src and dest file structure
                File srcFile = new File( src, file );
                File destFile = new File( dest, file );

                //recursive copy
                copyFolder( srcFile, destFile );
            }

        } else {

            //if file, then copy it (Use bytes stream to support all file types)
            InputStream in = new FileInputStream( src );
            OutputStream out = new FileOutputStream( dest );

            byte[] buffer = new byte[1024];

            int length;
            //copy the file content in bytes
            while ( ( length = in.read( buffer ) ) > 0 ) {
                out.write( buffer, 0, length );
            }

            in.close();
            out.close();
            UpdateAgent.logger.debug( "File copied from " + src + " to " + dest );
        }
    }

}