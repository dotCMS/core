package com.dotcms.autoupdater;

import org.apache.commons.cli.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class UpdateAgent {

    public static String MANIFEST_PROPERTY_AGENT_VERSION = "Agent-Version";
    //private static String MANIFEST_PROPERTY_RELEASE_VERSION = "Release-Version";

    public static Logger logger;
    public static boolean isDebug = false;
    public static String logFile;

    private String url;
    private String homeProjectPath;
    public static String FOLDER_HOME_DOTSERVER = "dotserver";
    public static String FOLDER_HOME_UPDATER = "autoupdater";
    public static String FOLDER_HOME_BACK_UP = "backup";
    //private String backupFile;
    private String proxy;
    private String proxyUser;
    private String proxyPass;
    private boolean allowTestingBuilds;
    private String newMinor;
    private String newVersion;

    private String MESSAGES_CONFIRM_TEXT = Messages.getString( "UpdateAgent.text.confirm" );
    private String MESSAGES_HELP_TEXT = Messages.getString( "UpdateAgent.text.help" );

    public static void main ( String[] args ) {
        new UpdateAgent().run( args );
    }

    public void run ( String[] args ) {

        UpdateOptions updateOptions = new UpdateOptions();
        Options options = updateOptions.getOptions();

        // create the parser
        CommandLineParser parser = new GnuParser();
        CommandLine line;
        try {
            // parse the command line arguments
            line = parser.parse( options, args );

        } catch ( MissingOptionException e ) {
            System.err.println( Messages.getString( "UpdateAgent.error.command.missing.options" ) );
            List<String> list = e.getMissingOptions();
            for ( String item : list ) {
                System.err.println( item );
            }
            return;
        } catch ( ParseException e ) {
            System.err.println( Messages.getString( "UpdateAgent.error.command.parsing" ) + e.getMessage() );
            return;
        }

        try {

            //Initializing properties
            configureLogger( line );

            //Command line parameters
            allowTestingBuilds = Boolean.parseBoolean( line.getOptionValue( UpdateOptions.ALLOW_TESTING_BUILDS ) );
            proxy = line.getOptionValue( UpdateOptions.PROXY );
            proxyUser = line.getOptionValue( UpdateOptions.PROXY_USER );
            proxyPass = line.getOptionValue( UpdateOptions.PROXY_PASS );
            url = line.getOptionValue( UpdateOptions.URL, updateOptions.getDefault( "update.url", "" ) );
            homeProjectPath = line.getOptionValue( UpdateOptions.HOME, System.getProperty( "user.dir" ) );
            updateOptions.setHomeFolder( homeProjectPath );
            updateOptions.setUpdateFilesFolder( homeProjectPath + File.separator + FOLDER_HOME_UPDATER + File.separator + "updates" );

            //Verify if we need to print the help documentation
            if ( line.hasOption( UpdateOptions.HELP ) || args.length == 0 ) {
                UpdateUtil.printHelp( options, MESSAGES_HELP_TEXT );
                return;
            }

            logger.info( Messages.getString( "UpdateAgent.text.dotcms.home" ) + getHomeProjectPath() );

            //Some validations...
            checkHome( getHomeProjectPath() + File.separator + FOLDER_HOME_DOTSERVER );
            checkRequisites( getHomeProjectPath() + File.separator + FOLDER_HOME_DOTSERVER );

            newMinor = "";
            newVersion = "";
            String version = getVersion();
            String minor = UpdateUtil.getBuildVersion( getJarProps() );
            /*SimpleDateFormat sdf = new SimpleDateFormat( "yyyMMdd_HHmm" );
            backupFile = line.getOptionValue( UpdateOptions.BACKUP, "update_backup_b" + minor + "_" + sdf.format( new Date() ) + ".zip" );
            if ( !backupFile.endsWith( ".zip" ) ) {
                backupFile += ".zip";
            }*/
            String agentVersion = UpdateUtil.getManifestValue( MANIFEST_PROPERTY_AGENT_VERSION );
            logger.debug( Messages.getString( "UpdateAgent.text.autoupdater.version" ) + agentVersion );

            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //+++++++++++++++++++++VERIFY FOR NEW VERSIONS OF THE AUTOUPDATER++++++++++++++++++++
            if ( !line.hasOption( UpdateOptions.NO_UPDATE ) ) {
                // Check to see if new version of the updater exists
                File newAgent = downloadAgent( version );
                if ( newAgent != null ) {
                    // Exit, We found an update for the autoupdater, the auto updater script will restart the process after the update of the autoupdater.jar
                    logger.info( Messages.getString( "UpdateAgent.text.new.autoupdater" ) );
                    System.exit( 0 );
                }
            }

            File updateFile = null;
            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //Verify if the user specified a version to update to
            if ( line.hasOption( UpdateOptions.SPECIFIC_VERSION ) ) {

                if ( minor != null ) {
                    logger.info( Messages.getString( "UpdateAgent.text.your.version" ) + version + " / " + minor + " Version " + line.getOptionValue( UpdateOptions.SPECIFIC_VERSION ) );
                } else {
                    logger.info( Messages.getString( "UpdateAgent.text.your.version" ) + version + " Version " + line.getOptionValue( UpdateOptions.SPECIFIC_VERSION ) );
                }

                Map<String, String> map = new HashMap<String, String>();
                map.put( "version", version );
                //map.put( "buildNumber", minor );
                map.put( "specificVersion", line.getOptionValue( UpdateOptions.SPECIFIC_VERSION ) );
                if ( allowTestingBuilds ) {
                    map.put( "allowTestingBuilds", "true" );
                }

                //Search for the update version
                updateFile = searchForVersion( map );
            }
            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //Verify if the user provided an update file
            else if ( line.hasOption( UpdateOptions.FILE ) ) {

                if ( minor != null ) {
                    logger.info( Messages.getString( "UpdateAgent.text.your.version" ) + version + " / " + minor + " file " + line.getOptionValue( UpdateOptions.FILE ) );
                } else {
                    logger.info( Messages.getString( "UpdateAgent.text.your.version" ) + version + " file " + line.getOptionValue( UpdateOptions.FILE ) );
                }

                // Use user provided file
                updateFile = new File( getHomeProjectPath() + File.separator + FOLDER_HOME_UPDATER + File.separator + "updates" + File.separator + line.getOptionValue( UpdateOptions.FILE ) );
                if ( !updateFile.exists() ) {
                    throw new UpdateException( Messages.getString( "UpdateAgent.error.file.not.found" ), UpdateException.ERROR );
                }

                // Get the version locally
                String fileMajor = UpdateUtil.getFileMayorVersion( updateFile );
                String fileMinor = UpdateUtil.getFileMinorVersion( updateFile );

                if ( fileMinor != null ) {
                    logger.info( Messages.getString( "UpdateAgent.text.file.version" ) + fileMajor + " / " + fileMinor );
                    newMinor = fileMinor;
                } else {
                    logger.info( Messages.getString( "UpdateAgent.text.file.version" ) + fileMajor );
                }
                newVersion = fileMajor;

                logger.info( " " );

            } else {

                // Download update file
                if ( minor != null ) {
                    logger.info( Messages.getString( "UpdateAgent.text.your.version" ) + version + " / " + minor );
                } else {
                    logger.info( Messages.getString( "UpdateAgent.text.your.version" ) + version );
                }

                if ( updateFile == null ) {

                    Map<String, String> map = new HashMap<String, String>();
                    map.put( "version", version );
                    //map.put( "buildNumber", minor );
                    if ( allowTestingBuilds ) {
                        map.put( "allowTestingBuilds", "true" );
                    }

                    //Search for the update version
                    updateFile = searchForVersion( map );
                }
            }

            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //Ok, now we have an update file to use...
            if ( updateFile != null && updateFile.exists() ) {

                FileUpdater fileUpdater = new FileUpdater( updateFile, getHomeProjectPath(), null, line.hasOption( UpdateOptions.DRY_RUN ) );

                // Pre-process the update, prepare everything and make back-ups
                fileUpdater.preUpdate();

                // Ask for confirmation
                UpdateUtil.confirm( line, MESSAGES_CONFIRM_TEXT );

                // Execute the update, unziping the downloaded file on the .dotserver/ directory and applying the required changes in there
                fileUpdater.doUpdate();

                //Clean up...
                fileUpdater.postUpdate();
                if ( newMinor != null && !newMinor.equals( "" ) ) {
                    throw new UpdateException( Messages.getString( "UpdateAgent.text.dotcms.dotcms.updated" ) + newVersion + " / " + newMinor, UpdateException.SUCCESS );
                } else {
                    throw new UpdateException( Messages.getString( "UpdateAgent.text.dotcms.dotcms.updated" ) + newVersion, UpdateException.SUCCESS );
                }
            }


        } catch ( IOException e ) {
            //Just in case it was left running
            ActivityIndicator.endIndicator();

            if ( isDebug ) {
                logger.debug( "IOException: ", e );
            } else {
                logger.error( Messages.getString( "UpdateAgent.error.downloading" ) + e.getMessage() );
            }

            logger.info( " " );

        } catch ( UpdateException e ) {
            //Just in case it was left running
            ActivityIndicator.endIndicator();
            if ( !e.getType().equals( UpdateException.CANCEL ) ) {
                if ( e.getType().equals( UpdateException.ERROR ) ) {
                    if ( isDebug ) {
                        logger.debug( "UpdateException: ", e );
                    } else {
                        logger.error( Messages.getString( "UpdateAgent.error.updating" ) + e.getMessage() );
                    }
                } else {
                    logger.info( e.getMessage() );
                }
            }

            logger.info( " " );
        }

    }

    /**
     * Search for an update version
     *
     * @param map
     * @throws UpdateException
     * @throws IOException
     */
    private File searchForVersion ( Map<String, String> map ) throws UpdateException, IOException {

        PostMethod method = doGet( url, map );
        int ret = method.getStatusCode();
        if ( ret == 200 ) {
            // Get the version of the jar.
            try {
                newMinor = method.getResponseHeader( "Minor-Version" ).getValue();
                if ( newMinor.trim().length() > 0 ) {
                    String[] minorArr = newMinor.split( "_" );
                    if ( minorArr.length > 1 ) {
                        logger.info( Messages.getString( "UpdateAgent.text.latest.version" ) + minorArr[0] + " / " + minorArr[1] );
                    } else {
                        logger.info( Messages.getString( "UpdateAgent.text.latest.version" ) + minorArr[0] );
                    }
                    newVersion = minorArr[0];
                    logger.info( " " );
                } else {
                    throw new Exception();
                }
            } catch ( Exception e ) {
                logger.debug( Messages.getString( "UpdateAgent.error.no.minor.version" ), e );
                throw new UpdateException( Messages.getString( "UpdateAgent.error.no.minor.version" ), UpdateException.ERROR );
            }

            String fileName = "update_" + newVersion + ".zip";
            File updateFile = new File( getHomeProjectPath() + File.separator + FOLDER_HOME_UPDATER + File.separator + "updates" + File.separator + fileName );
            if ( updateFile.exists() ) {
                //check md5 of file
                String MD5 = null;
                boolean hasMD5 = false;
                if ( method.getResponseHeader( "Content-MD5" ) != null && !method.getResponseHeader( "Content-MD5" ).equals( "" ) && !method.getResponseHeader( "Content-MD5" ).equals( "null" ) ) {
                    MD5 = method.getResponseHeader( "Content-MD5" ).getValue();
                    if ( !MD5.equals( "" ) ) {
                        hasMD5 = true;
                    }
                }
                if ( hasMD5 ) {
                    String dlMD5 = UpdateUtil.getMD5( updateFile );
                    logger.debug( Messages.getString( "UpdateAgent.debug.server.md5" ) + MD5 );
                    logger.debug( Messages.getString( "UpdateAgent.debug.file.md5" ) + dlMD5 );

                    if ( MD5 == null || MD5.length() == 0 || !dlMD5.equals( MD5 ) ) {
                        logger.fatal( Messages.getString( "UpdateAgent.error.md5.failed" ) );
                        throw new UpdateException( Messages.getString( "UpdateAgent.error.file.exists" ) + fileName, UpdateException.ERROR );
                    }
                } else {
                    // file verified, let's use it
                    logger.info( updateFile.getName() + ": " + Messages.getString( "UpdateAgent.text.md5.verified" ) );
                }

            } else {
                //Create the updates directory
                if ( !updateFile.getParentFile().exists() ) {
                    updateFile.getParentFile().mkdirs();
                }
                // Download the update content, the update servlet will provide an url for the update file
                String downloadUrl = method.getResponseHeader( "Download-Link" ).getValue();
                download( downloadUrl, updateFile, method );
            }

            return updateFile;

        } else {
            switch ( ret ) {
                case 204:
                    throw new UpdateException( Messages.getString( "UpdateAgent.text.dotcms.uptodate" ), UpdateException.SUCCESS );
                case 401:
                    throw new UpdateException( Messages.getString( "UpdateAgent.error.login.failed" ), UpdateException.ERROR );
                case 403:
                    throw new UpdateException( Messages.getString( "UpdateAgent.error.login.failed" ), UpdateException.ERROR );
                default:
                    throw new UpdateException( Messages.getString( "UpdateAgent.error.unexpected.http.code" ) + ret, UpdateException.ERROR );
            }
        }
    }

    /**
     * Will configure the logging for this tool
     *
     * @param line
     */
    private void configureLogger ( CommandLine line ) {

        Logger logRoot = Logger.getRootLogger();

        //File appender get all logs always
        //Console one get all on debug
        //	...gets errors on quiet
        //	...on normal it gets info from UpdateAgent only.
        ConsoleAppender app = new ConsoleAppender( new PatternLayout( "%m%n" ) );
        FileAppender logApp;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat( "yyyMMdd_HHmm" );
            logFile = "update_" + sdf.format( new Date() ) + ".log";
            if ( line.hasOption( UpdateOptions.LOG ) ) {
                logFile = line.getOptionValue( UpdateOptions.LOG );
            }
            logApp = new FileAppender( new PatternLayout( "%d %m%n" ), logFile );
            logRoot.addAppender( logApp );
            logApp.setThreshold( Level.DEBUG );
        } catch ( IOException e ) {
            System.err.println( Messages.getString( "UpdateAgent.error.cant.create.log" ) + e.getMessage() );
            e.printStackTrace();
            System.err.println( Messages.getString( "UpdateAgent.error.cant.create.log.will.continue" ) );
        }


        Logger l = Logger.getLogger( UpdateAgent.class );
        l.setLevel( Level.DEBUG );
        logRoot.setLevel( Level.INFO );

        if ( line.hasOption( UpdateOptions.VERBOSE ) ) {
            app.setThreshold( Level.DEBUG );
            logRoot.addAppender( app );
            isDebug = true;
        } else {
            l.addAppender( app );
            if ( line.hasOption( UpdateOptions.QUIET ) ) {
                app.setThreshold( Level.ERROR );
            } else {
                app.setThreshold( Level.INFO );
            }
        }
        logger = Logger.getLogger( UpdateAgent.class );
    }

    private File findJarFile () throws IOException, UpdateException {

        String[] libDirs = new String[]{
                File.separator + "common" + File.separator + "lib",
                File.separator + "dotCMS" + File.separator + "WEB-INF" + File.separator + "lib"
        };
        File libDir = null;
        for ( String libDirName : libDirs ) {
            File f = new File( getHomeProjectPath() + File.separator + FOLDER_HOME_DOTSERVER + File.separator + libDirName );
            if ( f.exists() && f.isDirectory() ) {
                libDir = f;
                break;
            }
        }
        if ( libDir == null ) {
            throw new UpdateException( Messages.getString( "UpdateAgent.error.jar.not.found" ), UpdateException.ERROR );
        }

        File[] dotCMSjars = libDir.listFiles( new FileFilter() {

            public boolean accept ( File pathname ) {
                String fileName = pathname.getName().toLowerCase();
                if ( fileName.startsWith( "dotcms_" ) && fileName.endsWith( ".jar" ) && (!fileName.startsWith( "dotcms_ant" )) ) {
                    return true;
                }
                return false;
            }

        } );
        if ( dotCMSjars.length > 1 ) {
            String jars = "";
            for ( File jar : dotCMSjars ) {
                jars += " " + jar.getName();
            }
            throw new UpdateException( Messages.getString( "UpdateAgent.error.multiple.jars" ) + jars, UpdateException.ERROR );

        }

        if ( dotCMSjars.length < 1 ) {
            throw new UpdateException( Messages.getString( "UpdateAgent.error.jar.not.found" ), UpdateException.ERROR );
        }

        return dotCMSjars[0];
    }


    private Properties getJarProps () throws IOException, UpdateException {

        JarFile jar = new JarFile( findJarFile() );
        JarEntry entry = jar.getJarEntry( "com/liferay/portal/util/build.properties" );
        Properties props = new Properties();
        InputStream in = jar.getInputStream( entry );
        props.load( in );
        return props;
    }

    private String getVersion () throws IOException, UpdateException {

        Properties props = getJarProps();
        return props.getProperty( "dotcms.release.version" );
    }

    /**
     * Will verify for some requires files on this update, specially the ant jars require then to run our ant tasks
     *
     * @param home
     * @return
     * @throws UpdateException
     */
    private boolean checkRequisites ( String home ) throws UpdateException {

        PostProcess postProcess = new PostProcess();
        postProcess.setHome( home );

        return postProcess.checkRequisites();
    }

    /**
     * This method will verify that we have a valid .dotserver/ structure and installation looking for a couple of files
     * that if it is a valid installation should be there.
     *
     * @param home
     * @return
     * @throws UpdateException
     */
    private boolean checkHome ( String home ) throws UpdateException {

        String[] homeCheckElements = {"build.xml", "dotCMS/WEB-INF/web.xml"};

        File homeFolder;
        for ( String check : homeCheckElements ) {
            homeFolder = new File( home + File.separator + check );
            if ( !homeFolder.exists() ) {
                throw new UpdateException( Messages.getString( "UpdateAgent.error.home.not.valid" ), UpdateException.ERROR );
            }
        }

        return true;
    }

    /**
     * Returns the update file for the autoupdater client
     *
     * @return Autoupdater client update file
     */
    private File downloadAgent ( String version ) {

        File updateFile = new File( getHomeProjectPath() + File.separator + FOLDER_HOME_UPDATER + File.separator + "autoUpdater.new" );

        try {

            updateFile.createNewFile();

            Map<String, String> map = new HashMap<String, String>();
            map.put( "version", version );
            //map.put( "version", UpdateUtil.getManifestValue( MANIFEST_PROPERTY_RELEASE_VERSION ) );
            map.put( "agent_version", UpdateUtil.getManifestValue( MANIFEST_PROPERTY_AGENT_VERSION ) );

            if ( allowTestingBuilds ) {
                map.put( "allowTestingBuilds", "true" );
            }
            //Talking with the update servlet....
            PostMethod method = doGet( url, map );

            //Download the update file
            int ret = download( updateFile, method );
            if ( ret == 200 ) {
                return updateFile;
            }
            if ( ret == 204 ) {
                logger.debug( Messages.getString( "UpdateAgent.text.autoupdater.uptodate" ) );
            }
        } catch ( IOException e ) {
            logger.error( Messages.getString( "UpdateAgent.error.no.autoupdater.version" ) + e.getMessage() );
            logger.debug( "IOException: ", e );
        }

        updateFile.delete();
        return null;
    }

    /**
     * Process a get method
     *
     * @param fileUrl
     * @param pars
     * @return
     * @throws IOException
     */
    private PostMethod doGet ( String fileUrl, Map<String, String> pars ) throws IOException {

        HttpClient client = new HttpClient();

        // Setup a proxy
        if ( proxy != null && proxy.length() > 0 ) {

            String proxyHost = proxy.substring( 0, proxy.indexOf( ":" ) );
            String proxyPort = proxy.substring( proxy.indexOf( ":" ) + 1 );

            client.getHostConfiguration().setProxy( proxyHost, Integer.parseInt( proxyPort ) );
            if ( proxyUser != null && proxyUser.length() > 0 && proxyPass != null && proxyPass.length() > 0 ) {
                // Authenticate with proxy
                client.getState().setProxyCredentials( null, null, new UsernamePasswordCredentials( proxyUser, proxyPass ) );
            }
        }

        PostMethod method = new PostMethod( fileUrl );
        Object[] keys = pars.keySet().toArray();
        NameValuePair[] data = new NameValuePair[keys.length];
        for ( int i = 0; i < keys.length; i++ ) {
            String key = (String) keys[i];
            NameValuePair pair = new NameValuePair( key, pars.get( key ) );
            data[i] = pair;
        }

        method.setRequestBody( data );
        client.executeMethod( method );

        return method;
    }

    /**
     * This method will download a file using a given url, after the download, if it was successful this method
     * will verify the integrity of the file using the md5 check sum.
     *
     * @param downloadUrl
     * @param outputFile
     * @param postMethod
     * @return status code
     */
    private int download ( String downloadUrl, File outputFile, PostMethod postMethod ) {

        int statusCode = 0;

        try {
            //Current status of the resent communication with the update servlet
            statusCode = postMethod.getStatusCode();

            logger.debug( Messages.getString( "UpdateAgent.debug.return.code" ) + statusCode );

            if ( statusCode == 200 ) {//If everything was ok.....

                // Just in case something else fails
                statusCode = -1;

                //Create objects to manage this file url...
                URL url = new URL( downloadUrl );
                URLConnection urlConnection = url.openConnection();

                //Getting remote file size
                int length = urlConnection.getContentLength();

                String newMinor = null;
                try {
                    newMinor = postMethod.getResponseHeader( "Minor-Version" ).getValue();
                } catch ( Exception ignored ) {
                }

                String downloadMessage = Messages.getString( "UpdateAgent.text.downloading" );
                if ( newMinor != null ) {

                    downloadMessage += Messages.getString( "UpdateAgent.text.new.minor" );

                    String[] minorArr = newMinor.split( "_" );
                    if ( minorArr.length > 1 ) {
                        downloadMessage += minorArr[0] + " / " + minorArr[1];
                    } else {
                        downloadMessage += minorArr[0];
                    }
                }
                if ( length > 0 ) {
                    downloadMessage += " (" + length / 1024 + "kB)";
                }
                logger.info( downloadMessage );

                //Configuration of the download process handler
                long startTime = System.currentTimeMillis();
                long refreshInterval = 500;
                DownloadProgress downloadProgress = new DownloadProgress( length );

                //Initializing the download.....
                InputStream is = urlConnection.getInputStream();
                OutputStream outStream = new FileOutputStream( outputFile );

                byte[] buffer = new byte[1024];
                int bytesRead, bytesWritten = 0;
                while ( (bytesRead = is.read( buffer )) != -1 ) {
                    outStream.write( buffer, 0, bytesRead );
                    bytesWritten += bytesRead;

                    //Keep tracking of the download status
                    long currentTime = System.currentTimeMillis();
                    if ( (currentTime - startTime) > refreshInterval ) {
                        String message = downloadProgress.getProgressMessage( bytesWritten, startTime, currentTime );
                        startTime = currentTime;
                        System.out.print( "\r" + message );
                    }
                }

                String message = Messages.getString( "UpdateAgent.text.download.complete" );
                System.out.print( "\r" + message );
                System.out.println( "" );
                outStream.close();
                is.close();

                //Now we need to verfy the integrity of this downloaded file using md5
                String MD5 = null;
                boolean hasMD5 = false;
                if ( postMethod.getResponseHeader( "Content-MD5" ) != null && !postMethod.getResponseHeader( "Content-MD5" ).equals( "" ) ) {
                    MD5 = postMethod.getResponseHeader( "Content-MD5" ).getValue();
                    if ( !MD5.equals( "" ) ) {
                        hasMD5 = true;
                    }
                }
                if ( hasMD5 ) {//The servlet sent us the md5 content, so lets use...
                    String dlMD5 = UpdateUtil.getMD5( outputFile );
                    logger.debug( Messages.getString( "UpdateAgent.debug.server.md5" ) + MD5 );
                    logger.debug( Messages.getString( "UpdateAgent.debug.file.md5" ) + dlMD5 );

                    if ( MD5 == null || MD5.length() == 0 || !dlMD5.equals( MD5 ) ) {
                        logger.fatal( Messages.getString( "UpdateAgent.error.md5.failed" ) );
                        outputFile.delete();
                    } else {
                        // everything went ok, we return the right return code
                        statusCode = 200;
                        logger.info( Messages.getString( "UpdateAgent.text.md5.verified" ) );
                    }
                } else {
                    statusCode = 200;
                    logger.info( Messages.getString( "UpdateAgent.text.md5.verified" ) );
                }
            }

            if ( statusCode == 204 ) {
                logger.debug( Messages.getString( "UpdateAgent.debug.no.content" ) );
            }

        } catch ( HttpException e ) {
            logger.error( Messages.getString( "UpdateAgent.error.downloading.file" ) + e.getMessage() );
            logger.debug( "HttpException: ", e );
        } catch ( IOException e ) {
            logger.error( Messages.getString( "UpdateAgent.error.downloading.file" ) + e.getMessage() );
            logger.debug( "IOException: ", e );
        }
        if ( postMethod != null ) {
            postMethod.releaseConnection();
        }

        return statusCode;
    }

    /**
     * This method will download an update file for dotcms, this will be after establishing a connection with the update servlet who is the one that will provide this update file
     *
     * @param outputFile
     * @param method
     * @return
     * @throws URIException
     * @throws MalformedURLException
     */
    private int download ( File outputFile, PostMethod method ) throws URIException, MalformedURLException {

        int statusCode = 0;
        try {

            statusCode = method.getStatusCode();
            logger.debug( Messages.getString( "UpdateAgent.debug.return.code" ) + statusCode );
            if ( statusCode == 200 ) {

                // Just in case something else fails
                statusCode = -1;

                InputStream is = method.getResponseBodyAsStream();

                OutputStream out = new FileOutputStream( outputFile );
                byte[] b = new byte[1024];
                int len;
                int count = 0;
                long length = 0;
                String newMinor = null;

                try {
                    String lenghtString = method.getResponseHeader( "Content-Length" ).getValue();
                    length = Long.parseLong( lenghtString );
                } catch ( Exception ignored ) {
                }

                try {
                    newMinor = method.getResponseHeader( "Minor-Version" ).getValue();
                } catch ( Exception ignored ) {
                }

                String dlMessage = Messages.getString( "UpdateAgent.text.downloading" );
                if ( newMinor != null ) {
                    dlMessage += Messages.getString( "UpdateAgent.text.new.minor" );
                    dlMessage += newMinor;
                }
                if ( length > 0 ) {
                    dlMessage += " (" + length / 1024 + "kB)";
                }
                logger.info( dlMessage );
                long startTime = System.currentTimeMillis();
                long refreshInterval = 500;
                DownloadProgress dp = new DownloadProgress( length );
                while ( (len = is.read( b )) != -1 ) {
                    for ( int i = 0; i < len; i++ ) {
                        out.write( (char) b[i] );
                        count++;
                    }
                    long currentTime = System.currentTimeMillis();

                    if ( (currentTime - startTime) > refreshInterval ) {
                        String message = dp.getProgressMessage( count, startTime, currentTime );
                        startTime = currentTime;
                        System.out.print( "\r" + message );
                    }
                }
                String message = dp.getProgressMessage( count, startTime, System.currentTimeMillis() );
                System.out.print( "\r" + message );
                System.out.println( "" );
                out.close();
                is.close();

                // verfiy md5
                String MD5 = null;
                boolean hasMD5 = false;
                if ( method.getResponseHeader( "Content-MD5" ) != null && !method.getResponseHeader( "Content-MD5" ).equals( "" ) ) {
                    MD5 = method.getResponseHeader( "Content-MD5" ).getValue();
                    if ( !MD5.equals( "" ) ) {
                        hasMD5 = true;
                    }
                }
                if ( hasMD5 ) {
                    String dlMD5 = UpdateUtil.getMD5( outputFile );
                    logger.debug( Messages.getString( "UpdateAgent.debug.server.md5" ) + MD5 );
                    logger.debug( Messages.getString( "UpdateAgent.debug.file.md5" ) + dlMD5 );

                    if ( MD5 == null || MD5.length() == 0 || !dlMD5.equals( MD5 ) ) {
                        logger.fatal( Messages.getString( "UpdateAgent.error.md5.failed" ) );
                        outputFile.delete();
                    } else {
                        // everything went ok, we return the right return code
                        statusCode = 200;
                        logger.info( Messages.getString( "UpdateAgent.text.md5.verified" ) );
                    }
                } else {
                    statusCode = 200;
                    logger.info( Messages.getString( "UpdateAgent.text.md5.verified" ) );
                }
            }

            if ( statusCode == 204 ) {
                logger.debug( Messages.getString( "UpdateAgent.debug.no.content" ) );
            }
        } catch ( HttpException e ) {
            logger.error( Messages.getString( "UpdateAgent.error.downloading.file" ) + e.getMessage() );
            logger.debug( "HttpException: ", e );
        } catch ( IOException e ) {
            logger.error( Messages.getString( "UpdateAgent.error.downloading.file" ) + e.getMessage() );
            logger.debug( "IOException: ", e );
        }
        if ( method != null ) {
            method.releaseConnection();
        }

        return statusCode;
    }

    public String getHomeProjectPath () {
        return homeProjectPath;
    }

}