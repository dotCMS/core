package com.dotmarketing.osgi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;

import javax.servlet.ServletContext;

import org.apache.commons.io.FilenameUtils;
import org.apache.felix.http.api.ExtHttpService;
import org.apache.struts.Globals;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.config.impl.ModuleConfigImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.dotmarketing.util.Config;
import com.liferay.portal.util.Constants;
import com.liferay.util.FileUtil;

class ActivatorUtil {

	static final String PATH_SEPARATOR = "/";
    static final String OSGI_FOLDER = "/osgi";
    static final String VELOCITY_FOLDER = "/WEB-INF/velocity";

    static UrlOsgiClassLoader findCustomURLLoader ( ClassLoader loader ) {

        if ( loader == null ) {
            return null;
        } else if ( loader instanceof UrlOsgiClassLoader ) {
            return (UrlOsgiClassLoader) loader;
        } else {
            return findCustomURLLoader( loader.getParent() );
        }
    }

    static String getBundleFolder ( BundleContext context ) {

        //We will use the bundle jar name as the folder name for the osgi resources we move inside dotCMS
        String bundleLocation = context.getBundle().getLocation();
        String jarFileName = FilenameUtils.getName( bundleLocation );
        jarFileName = jarFileName.replace( ".jar", "" );

        //return OSGI_FOLDER + File.separator + context.getBundle().getBundleId();
        return OSGI_FOLDER + File.separator + jarFileName;
    }

    static ModuleConfig getModuleConfig () {
        ServletContext servletContext = Config.CONTEXT;
        return (ModuleConfig) servletContext.getAttribute( Globals.MODULE_KEY );
    }

    /**
     * In order to be able to add ActionMappings we need to unfreeze the module config, that freeze status don't allow modifications.
     *
     * @param moduleConfig
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    static void unfreeze ( ModuleConfig moduleConfig ) throws NoSuchFieldException, IllegalAccessException {

        Field configuredField = ModuleConfigImpl.class.getDeclaredField( "configured" );//We need to access it using reflection
        configuredField.setAccessible( true );
        configuredField.set( moduleConfig, false );
        configuredField.setAccessible( false );
    }

    /**
     * Deletes the generated folders and copied files inside dotCMS for this bundle
     *
     * @param context
     */
    static void cleanResources ( BundleContext context ) {

        ServletContext servletContext = Config.CONTEXT;

        //Cleaning the resources under the html folder
        String resourcesPath = servletContext.getRealPath( Constants.TEXT_HTML_DIR + getBundleFolder( context ) );
        File resources = new File( resourcesPath );
        if ( resources.exists() ) {
            FileUtil.deltree( resources );
        }

        //Now cleaning the resources under the velocity folder
        resourcesPath = servletContext.getRealPath( VELOCITY_FOLDER + getBundleFolder( context ) );
        resources = new File( resourcesPath );
        if ( resources.exists() ) {
            FileUtil.deltree( resources );
        }
    }

    /**
     * Util method to copy all the resources inside the folder of the given resource to the corresponding velocity dotCMS folders
     *
     * @param context
     * @param referenceResourcePath reference resource to get its container folder and move the resources inside that folder
     * @throws Exception
     */
    static void moveVelocityResources ( BundleContext context, String referenceResourcePath ) throws Exception {

        ServletContext servletContext = Config.CONTEXT;
        String destinationPath = servletContext.getRealPath( VELOCITY_FOLDER + getBundleFolder( context ) );

        moveResources( context, referenceResourcePath, destinationPath );
    }

    /**
     * Util method to copy all the resources inside the folder of the given resource to the corresponding dotCMS folders
     *
     * @param context
     * @param referenceResourcePath reference resource to get its container folder and move the resources inside that folder
     * @throws Exception
     */
    static void moveResources ( BundleContext context, String referenceResourcePath ) throws Exception {

        ServletContext servletContext = Config.CONTEXT;
        String destinationPath = servletContext.getRealPath( Constants.TEXT_HTML_DIR + getBundleFolder( context ) );

        moveResources( context, referenceResourcePath, destinationPath );
    }

    /**
     * Util method to copy all the resources inside a folder to a given destination
     *
     * @param context
     * @param referenceResourcePath reference resource to get its container folder and move the resources inside that folder
     * @param destinationPath
     * @throws Exception
     */
    private static void moveResources ( BundleContext context, String referenceResourcePath, String destinationPath ) throws Exception {

        //Get the container folder of the given resource
        String containerFolder = getContainerFolder( referenceResourcePath );

        //Find all the resources under that folder
        Enumeration<URL> entries = context.getBundle().findEntries( containerFolder, "*.*", true );
        while ( entries.hasMoreElements() ) {

            URL entryUrl = entries.nextElement();
            String entryPath = entryUrl.getPath();

            String resourceFilePath = destinationPath + entryPath;
            File resourceFile = new File( resourceFilePath );
            if ( !resourceFile.exists() ) {

                InputStream in = null;
                OutputStream out = null;
                try {
                    if ( !resourceFile.getParentFile().exists() ) {
                        resourceFile.getParentFile().mkdirs();
                    }
                    resourceFile.createNewFile();

                    in = entryUrl.openStream();
                    out = new FileOutputStream( resourceFile );

                    byte[] buffer = new byte[1024];
                    int length;
                    while ( (length = in.read( buffer )) > 0 ) {
                        out.write( buffer, 0, length );
                    }

                } finally {
                    if ( in != null ) {
                        in.close();
                    }
                    if ( out != null ) {
                        out.flush();
                        out.close();
                    }
                }
            }

        }

    }

    /**
     * Unregister a servlet using a given mapping servlet
     *
     * @param context
     * @throws Exception
     */
    @SuppressWarnings ("unchecked")
    static void unregisterAll ( BundleContext context ) throws Exception {

        ServiceReference sRef = context.getServiceReference( ExtHttpService.class.getName() );
        if ( sRef != null ) {

            /*
             Why don't use it in the same way as the activators???, classpaths :)

             On the felix framework initialization dotCMS loads this class (ExtHttpService) using its own ClassLoader.
             So I can't use directly this class and its implementation because on this class felix can't use its
             instance (Created with its own ClassLoader because the dotCMS ClassLoader already loaded the same class,
             meaning we have in memory a definition that is not the one provided by felix and for that reason they are different, nice... :) ),
             That will cause runtime errors and that's why we use reflection.
             */

            //ExtHttpService httpService = (ExtHttpService) context.getService( sRef );
            Object httpService = context.getService( sRef );

            //Now invoke the method that will unregister all the registered Servlets and Filters
            Method unregisterAllMethod = httpService.getClass().getMethod( "unregisterAll" );
            unregisterAllMethod.invoke( httpService );
        }
    }

    /**
     * Util method to get the container folder of a given resource inside this bundle
     *
     * @param path
     * @return
     */
    private static String getContainerFolder ( String path ) {

        if ( !path.startsWith( PATH_SEPARATOR ) ) {
            path = PATH_SEPARATOR + path;
        }

        int index = path.indexOf( PATH_SEPARATOR );
        if ( path.startsWith( PATH_SEPARATOR ) ) {
            index = path.indexOf( PATH_SEPARATOR, 1 );
        }

        return path.substring( 1, index + 1 );
    }

    static String getManifestHeaderValue ( BundleContext context, String key ) {
        return context.getBundle().getHeaders().get( key );
    }

}

