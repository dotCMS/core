package com.dotmarketing.plugin.util;

import com.dotmarketing.util.UtilMethods;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This plugin will allow any file to be overridden or added.
 * <p/>
 * Under the root of a plugin you will have a directory named ROOT. If you wanted to override the server.xml it would look like this
 * <b>./plugins/com.dotcms.conf/ROOT/tomcat/conf/server.xml</b>
 * <p/>
 * Then the original file would get moved to <b>_original/tomcat/conf/server.xml</b> if there is not already a file under that path
 * <p/>
 * On undeploy of plugins these files get switched back, and whatever is under the <b>_original/</b> folder will overwrite whatever is under <b>/</b>.
 *
 * @author Jonathan Gamba.
 *         Date: 4/12/12
 *         Time: 1:03 PM
 * @see com.dotmarketing.plugin.ant.DeployTask
 * @see com.dotmarketing.plugin.ant.UndeployTask
 */
public class PluginRoot {

    private static Logger logger = Logger.getLogger( PluginRoot.class );

    private String[] executableFiles = new String[]{"sh", "bat", "exe"};

    public static String ROOT_FOLDER = "ROOT";
    public static String BACKUP_FOLDER = "_original";

    private String rootPath;
    private String pluginsPath;
    private String backUpPath;
    private Collection<File> plugins;

    public PluginRoot (String rootPath, String pluginsPath) {

        setRootPath( rootPath );
        setPluginsPath( pluginsPath );

        Boolean canContinue = false;

        //Create if needed the back-up folder
        File backUpFolder = new File( getBackUpPath() );
        if ( !backUpFolder.exists() ) {
            Boolean success = backUpFolder.mkdir();
            if ( success ) {
                canContinue = true;
                logger.debug( "Back-up folder created at: " + backUpFolder.getAbsolutePath() );
            } else {
                logger.error( "Error creating Back-up folder: " + backUpFolder.getAbsolutePath() );
            }
        } else {
            canContinue = true;
        }

        if ( canContinue ) {
            //Getting all the current plugins
            setPlugins( PluginUtil.getPluginJars( rootPath, pluginsPath ) );
        }
    }

    /**
     * This method will be call it by the deploy-plugin ant task and will allow any file to be overridden or added.
     * <p/>
     * Under the root of a plugin you will have a directory named ROOT. If you wanted to override the server.xml it would look like this
     * <b>./plugins/com.dotcms.conf/ROOT/tomcat/conf/server.xml</b>
     * <p/>
     * Then the original file would get moved to <b>_original/tomcat/conf/server.xml</b> if there is not already a file under that path.
     * <p/>
     * <p/> What this method does:
     * <ul>
     * <li>Get the files under the ROOT folder inside the plugin. Only files are allowed, not empty folders</li>
     * <li>Back-up the original file, if a back-up exist leave the back-up file at it is</li>
     * <li>Copy the ROOT/xxx/xxx.xxx file to the original path</li>
     * </ul>
     */
    public void deploy () {

        if ( getPlugins() != null ) {
            for ( File plugin : getPlugins() ) {

                try {
                    String pluginName = PluginUtil.getPluginNameFromJar( plugin.getName() );
                    logger.debug( "---------------------------------------------------------------" );
                    logger.debug( "Overriding/Adding ROOT files for " + pluginName + " plugin..." );

                    //Use it as it is, a jar file
                    JarFile pluginJar = new JarFile( plugin );

                    //Moving ROOT files
                    moveForPlugin( pluginJar );

                } catch ( IOException e ) {
                    logger.error( "IOException: "
                            + e.getMessage() + " while moving ROOT files for: "
                            + plugin.getAbsolutePath(), e );
                }
            }
        }
    }

    /**
     * Move ROOT files for a given plugin, refer to method: {@link PluginRoot#deploy()}
     *
     * @param pluginJar
     * @throws IOException
     * @see PluginRoot#deploy()
     */
    private void moveForPlugin (JarFile pluginJar) throws IOException {

        //Now we need to get all the files under the ROOT folder of the plugin, for now lets focus in files with extension ie: ROOT/folder/folder/fileName.xyz
        //Directories with out files are going to be ignore
        String[] rootFilesPaths = PluginUtil.listFiles( ROOT_FOLDER + ".+\\..+", pluginJar );
        for ( String rootFilePath : rootFilesPaths ) {

            //Getting the relative path based on the ROOT path, ie: ROOT/tomcat/conf/server.xml --> tomcat/conf/server.xml
            String relativeFilePath = rootFilePath.replace( ROOT_FOLDER + File.separator, "" );
            File backUpFile = new File( getAbsoluteBackUpPath( relativeFilePath ) );//The possible back-up path for this file
            File originalFile = new File( getAbsolutePath( relativeFilePath ) );//The path of the original file to be override/add it

            logger.debug( "----" );
            logger.debug( "For: " + rootFilePath );

            //Verify if exist the original file we want to add/replace
            if ( originalFile.exists() ) {

                //Folder, files verifications
                File backUpFileDirectory = new File( backUpFile.getParent() );
                if ( !backUpFileDirectory.exists() ) {//If the back-up destiny directories doesn't exist we must create them
                    Boolean success = backUpFileDirectory.mkdirs();
                    if ( success ) {
                        logger.debug( "Created back-up folder: " + backUpFileDirectory.getAbsolutePath() );
                    } else {
                        logger.error( "Error creating back-up folder: " + backUpFileDirectory.getAbsolutePath() );
                    }
                }

                //Ok, if the back-up file doesn't exist the first we need to do is to create it, I mean create a back up for the original file...
                if ( !backUpFile.exists() ) {
                    //Moving the original file to the back-up folder
                    Boolean success = originalFile.renameTo( new File( backUpFileDirectory, backUpFile.getName() ) );
                    if ( success ) {
                        logger.debug( "Created back-up file: " + backUpFile.getAbsolutePath() );
                    } else {
                        logger.error( "Error creating back-up file: " + backUpFile.getAbsolutePath() );
                    }
                } //If we already have a back up just leave that way...
            } else {//The original file we want to add/replace doesn't exist, so create the directories for this file

                //Folder, files verifications
                File originalFileDirectory = new File( originalFile.getParent() );
                if ( !originalFileDirectory.exists() ) {//If the directories of the file we want to replace/add doesn't exist create it
                    Boolean success = originalFileDirectory.mkdirs();
                    if ( success ) {
                        logger.debug( "Created folder: " + originalFileDirectory.getAbsolutePath() );
                    } else {
                        logger.error( "Error creating folder: " + originalFileDirectory.getAbsolutePath() );
                    }
                }
            }

            //Ok, now lets copy our ROOT file to the project
            try {
                //Copying....
                JarEntry entry = pluginJar.getJarEntry( rootFilePath );
                copyContent( pluginJar.getInputStream( entry ), originalFile );

                logger.debug( "Replaced/added file: " + originalFile.getAbsolutePath() );
            } catch ( IOException e ) {
                logger.error( "Error replacing/adding back-up file: " + originalFile.getAbsolutePath(), e );
            }
        }
    }

    /**
     * On undeploy of plugins these files get switched back, and whatever is under the <b>_original/</b> folder will overwrite whatever is under <b>/</b>.
     */
    public void undeploy () {

        //Getting the back-up folder
        File backUpFileDirectory = new File( getBackUpPath() );

        //If exist lets restore all the files in it, if don't well.., nothing to do...
        if ( backUpFileDirectory.exists() ) {

            logger.debug( "---------------------------------------------------------------" );
            logger.debug( "Restoring back-up files under " + getBackUpPath() + " folder..." );

            //Now restore all the files under the back-up folder
            restoreFilesUnder( backUpFileDirectory );

            //Finally delete the back-up folder...
            Boolean success = deleteDirectory( backUpFileDirectory );
            if ( success ) {
                logger.debug( "Deleted back-up directory: " + backUpFileDirectory.getAbsolutePath() );
            } else {
                logger.error( "Error deleting back-up directory: " + backUpFileDirectory.getAbsolutePath() );
            }
        }

    }

    /**
     * This method will process only files under a given directory, will make it recursively, once it find a file it will call the {@link PluginRoot#restore(java.io.File)} method
     *
     * @param dir
     */
    private void restoreFilesUnder (File dir) {

        if ( dir.isDirectory() ) {
            String[] children = dir.list();
            for ( String aChildren : children ) {
                restoreFilesUnder( new File( dir, aChildren ) );
            }
        } else {
            //We found a file, restore it!
            restore( dir );
        }
    }

    /**
     * This method will delete a given folder, it will make it recursively removing from the children to the parent
     *
     * @param dir
     * @return
     */
    public static boolean deleteDirectory (File dir) {

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
     * On undeploy of plugins these files get switched back, and whatever is under the <b>_original/</b> folder will overwrite whatever is under <b>/</b>.
     * <p/>This method will restore a given back-up file
     *
     * @param fileToRestore
     */
    private void restore (File fileToRestore) {

        //Back-up file path
        String backUpFilePath = fileToRestore.getAbsolutePath();
        //Relative path for the files
        String relativeFilePath = backUpFilePath.substring( backUpFilePath.indexOf( BACKUP_FOLDER ) + (BACKUP_FOLDER + File.separator).length() );
        //Original path
        String originalPath = getAbsolutePath( relativeFilePath );

        logger.debug( "----" );
        logger.debug( "For: " + backUpFilePath );

        //First we need to delete the original file, we don't need it, we are going to restore the back-up file
        File originalFile = new File( originalPath );
        if ( originalFile.exists() ) {
            Boolean success = originalFile.delete();
            if ( success ) {
                logger.debug( "Deleted file: " + originalFile.getAbsolutePath() );
            } else {
                logger.error( "Error deleting file: " + originalFile.getAbsolutePath() );
            }
        }

        //If the folders for the original file doesn't exist create them..., actually this shouldn't happen...
        File originalFileDirectory = new File( originalFile.getParent() );
        if ( !originalFileDirectory.exists() ) {
            Boolean success = originalFileDirectory.mkdirs();
            if ( success ) {
                logger.debug( "Created folder: " + originalFileDirectory.getAbsolutePath() );
            } else {
                logger.error( "Error creating folder: " + originalFileDirectory.getAbsolutePath() );
            }
        }

        //Now lets move the back-up file to his otrginal place
        Boolean success = fileToRestore.renameTo( new File( originalFileDirectory, originalFile.getName() ) );
        if ( success ) {
            logger.debug( "Restored file: " + fileToRestore.getAbsolutePath() );
        } else {
            logger.error( "Error restoring file: " + fileToRestore.getAbsolutePath() );
        }
    }

    /**
     * Copy a given InputStream into a given destination File
     *
     * @param inputStream
     * @param destination
     * @throws IOException
     */
    private void copyContent (InputStream inputStream, File destination) throws IOException {

        //For Overwrite the file.
        OutputStream out = new FileOutputStream( destination );

        byte[] buf = new byte[1024];
        int len;
        while ( (len = inputStream.read( buf )) > 0 ) {
            out.write( buf, 0, len );
        }
        inputStream.close();
        out.close();

        //Now, lets try to add specific permissions for some specific type of files
        String fileExtension = UtilMethods.getFileExtension( destination.getName() );
        try {
            Collection<String> executables = Arrays.asList( executableFiles );
            if ( executables.contains( fileExtension ) ) {
                logger.debug( "Adding execution permissions to file: " + destination.getAbsolutePath() );

                //For linux lets try to do something more...
                if ( SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_UNIX ) {
                    Runtime.getRuntime().exec( "chmod 775 " + destination.getAbsolutePath() );
                } else {
                    destination.setReadable( true );
                    destination.setWritable( true );
                    destination.setExecutable( true );
                }
            }
        } catch ( Exception e ) {
            logger.error( "Error adding permissions to file.", e );
        }
    }

    /**
     * Concatenates a given path to the absolute path of the project
     *
     * @param path
     * @return
     */
    public String getAbsolutePath (String path) {
        File parentFolder = new File( getRootPath() );
        return parentFolder.getParent() + File.separator + path;
    }

    /**
     * Concatenates a given path to the absolute path of the Back-Up folder
     *
     * @param path
     * @return
     */
    public String getAbsoluteBackUpPath (String path) {
        return getBackUpPath() + File.separator + path;
    }

    /**
     * Returns the absolute path of the Back-Up folder
     *
     * @return
     */
    public String getBackUpPath () {
        return getAbsolutePath( BACKUP_FOLDER );
    }

    public String getRootPath () {
        return rootPath;
    }

    private void setRootPath (String rootPath) {
        this.rootPath = rootPath;
    }

    public String getPluginsPath () {
        return pluginsPath;
    }

    private void setPluginsPath (String pluginsPath) {
        this.pluginsPath = pluginsPath;
    }

    public Collection<File> getPlugins () {
        return plugins;
    }

    private void setPlugins (Collection<File> plugins) {
        this.plugins = plugins;
    }

}