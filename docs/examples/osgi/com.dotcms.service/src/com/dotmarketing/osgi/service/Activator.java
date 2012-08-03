package com.dotmarketing.osgi.service;

import com.dotmarketing.osgi.GenericBundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Hashtable;

/**
 * Created by Jonathan Gamba
 * Date: 6/18/12
 */
public class Activator extends GenericBundleActivator {

    /**
     * Implements BundleActivator.start(). Registers an
     * instance of a test service using the bundle context;
     * attaches properties to the service that can be queried
     * when performing a service look-up.
     *
     * @param bundleContext the framework context for the bundle.
     */
    @Override
    public void start ( BundleContext bundleContext ) throws Exception {

        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( "Language", "English" );

        bundleContext.registerService( HelloWorld.class.getName(), new HelloWorldService(), props );
    }

    /**
     * Implements BundleActivator.stop(). Does nothing since
     * the framework will automatically unregister any registered services.
     *
     * @param bundleContext the framework context for the bundle.
     */
    @Override
    public void stop ( BundleContext bundleContext ) throws Exception {
        // NOTE: The service is automatically unregistered.
    }

}