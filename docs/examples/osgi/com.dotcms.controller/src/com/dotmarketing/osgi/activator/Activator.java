package com.dotmarketing.osgi.activator;

import com.dotmarketing.filters.CMSFilter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.springframework.web.servlet.DispatcherServlet;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by Jonathan Gamba
 * Date: 6/18/12
 */
public class Activator implements BundleActivator {

    @SuppressWarnings ("unchecked")
    public void start ( BundleContext context ) throws Exception {

        ServiceReference sRef = context.getServiceReference( HttpService.class.getName() );
        if ( sRef != null ) {

            //Adding the library to the application classpath
            addFileToClasspath( "/home/jonathan/Projects/dotCMS/repository/git/dotCMS/dotCMS/WEB-INF/felix/load/bundle-com.dotcms.controller.lib.jar" );

            HttpService service = (HttpService) context.getService( sRef );
            try {
                DispatcherServlet ds = new DispatcherServlet();
                ds.setContextConfigLocation( "spring/example-servlet.xml" );
                service.registerServlet( "/spring", ds, null, null );
            } catch ( Exception e ) {
                e.printStackTrace();
            }
            CMSFilter.addExclude( "/dynamic/spring" );
        }
    }

    /**
     * Adds a file to the classpath.
     *
     * @param filePath a String pointing to the file
     * @throws IOException
     */
    private void addFileToClasspath ( String filePath ) throws Exception {

        File fileToAdd = new File( filePath );
        addFileToClasspath( fileToAdd );
    }

    /**
     * Adds a file to the classpath
     *
     * @param toAdd the file to be added
     * @throws IOException
     */
    private void addFileToClasspath ( File toAdd ) throws Exception {

        addURLToApplicationClassLoader( toAdd.toURI().toURL() );
    }

    private void addURLToApplicationClassLoader ( URL url ) throws IntrospectionException {

        ClassLoader currentThreadClassLoader = Thread.currentThread().getContextClassLoader();

        // Add the given url to the classpath. -Chain the current thread classloader
        URLClassLoader urlClassLoader = new URLClassLoader( new URL[]{url}, currentThreadClassLoader );

        // Replace the thread classloader - assumes you have permissions to do so
        Thread.currentThread().setContextClassLoader( urlClassLoader );
    }

    public void stop ( BundleContext context ) throws Exception {
        CMSFilter.removeExclude( "/dynamic/spring" );
    }

}