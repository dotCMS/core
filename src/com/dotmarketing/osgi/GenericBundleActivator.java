package com.dotmarketing.osgi;

import com.dotmarketing.util.Config;
import org.osgi.framework.BundleActivator;

import java.beans.IntrospectionException;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by Jonathan Gamba
 * Date: 7/23/12
 */
public abstract class GenericBundleActivator implements BundleActivator {

    /**
     * Allow to this bundle/elements to be visible and accessible from the host classpath
     */
    public void publishBundleServices () {

        //Felix classloader
        ClassLoader felixClassLoader = getFelixClassLoader();

        //Create a new class loader where we can "combine" our classloaders
        CombinedLoader combinedLoader = new CombinedLoader();
        combinedLoader.addLoader( Thread.currentThread().getContextClassLoader() );
        combinedLoader.addLoader( felixClassLoader );

        //Use this new "combined" class loader
        Thread.currentThread().setContextClassLoader( combinedLoader );
    }

    /**
     * Unpublish this bundle elements
     */
    public void unpublishBundleServices () {

        //Get the current classloader
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if ( classLoader instanceof CombinedLoader ) {

            //Try to remove this class loader
            ClassLoader felixClassLoader = getFelixClassLoader();
            ((CombinedLoader) classLoader).removeLoader( felixClassLoader );
        }
    }

    /**
     * Register a bundle library, this library must be a bundle inside the felix load folder.
     *
     * @param bundleJarFileName bundle file name
     * @throws Exception
     */
    public void registerBundleLibrary ( String bundleJarFileName ) throws Exception {

        //Felix directories
        String felixDirectory = Config.CONTEXT.getRealPath( File.separator + "WEB-INF" + File.separator + "felix" );
        String autoLoadDirectory = felixDirectory + File.separator + "load";

        //Adding the library to the application classpath
        addFileToClasspath( autoLoadDirectory + File.separator + bundleJarFileName );
    }

    /**
     * Adds a file to the classpath.
     *
     * @param filePath a String pointing to the file
     * @throws java.io.IOException
     */
    public void addFileToClasspath ( String filePath ) throws Exception {

        File fileToAdd = new File( filePath );
        addFileToClasspath( fileToAdd );
    }

    /**
     * Adds a file to the classpath
     *
     * @param toAdd the file to be added
     * @throws java.io.IOException
     */
    public void addFileToClasspath ( File toAdd ) throws Exception {

        addURLToApplicationClassLoader( toAdd.toURI().toURL() );
    }

    private void addURLToApplicationClassLoader ( URL url ) throws IntrospectionException {

        ClassLoader currentThreadClassLoader = Thread.currentThread().getContextClassLoader();

        // Add the given url to the classpath. -Chain the current thread classloader
        URLClassLoader urlClassLoader = new URLClassLoader( new URL[]{url}, currentThreadClassLoader );

        // Replace the thread classloader - assumes you have permissions to do so
        Thread.currentThread().setContextClassLoader( urlClassLoader );
    }

    private ClassLoader getFelixClassLoader () {
        return this.getClass().getClassLoader();
    }

}