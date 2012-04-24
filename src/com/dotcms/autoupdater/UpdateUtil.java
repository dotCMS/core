package com.dotcms.autoupdater;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;


public class UpdateUtil {

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

    public static boolean unzipDirectory ( File zipFile, String directoryName, String home, boolean dryrun ) throws IOException {

        ActivityIndicator.startIndicator();
        FileInputStream fis = new FileInputStream( zipFile );
        ZipInputStream zis = new ZipInputStream( new BufferedInputStream( fis ) );

        ZipEntry entry;
        BufferedOutputStream dest;
        int BUFFER = 1024;
        while ( ( entry = zis.getNextEntry() ) != null ) {
            if ( !entry.isDirectory() ) {
                if ( directoryName == null || entry.getName().startsWith( directoryName ) ) {

                    UpdateAgent.logger.debug( Messages.getString( "FileUpdater.debug.extract.file" ) + entry );

                    if ( !dryrun ) {

                        int count;
                        byte data[] = new byte[BUFFER];
                        File destFile = new File( home + File.separator + entry.getName() );
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

        while ( ( entry = zis.getNextEntry() ) != null ) {
            String entryName1 = entry.getName();
            if ( entryName1.startsWith( "dotCMS/WEB-INF/lib/dotcms_" ) && !entryName1.startsWith( "dotCMS/WEB-INF/lib/dotcms_ant" ) ) {

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

    public static Integer getFileMinorVersion ( File zipFile ) throws IOException, UpdateException {

        Properties props = getInnerFileProps( zipFile );
        String prop = props.getProperty( "dotcms.release.build" );

        if ( prop != null && !prop.equals( "" ) ) {
            return Integer.parseInt( prop );
        }
        throw new UpdateException( Messages.getString( "UpdateUtil.error.no.version" ), UpdateException.ERROR );
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

    public static boolean confirmUI (String confirmText) {

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

}