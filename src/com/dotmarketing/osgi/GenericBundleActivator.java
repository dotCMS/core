package com.dotmarketing.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Hashtable;

/**
 * Created by Jonathan Gamba
 * Date: 6/18/12
 */
public class GenericBundleActivator implements BundleActivator {

    private Class bundleInterface;
    private Object bundleImpl;
    Hashtable<String, String> props;

    public GenericBundleActivator ( Class bundleInterface, Object bundleImpl ) {
        this.bundleInterface = bundleInterface;
        this.bundleImpl = bundleImpl;
    }

    public Hashtable<String, String> getProperties () {
        return props;
    }

    public void setProperties ( Hashtable<String, String> props ) {
        this.props = props;
    }

    /**
     * Implements BundleActivator.start(). Registers an
     * instance of a dictionary service using the bundle context;
     * attaches properties to the service that can be queried
     * when performing a service look-up.
     *
     * @param bundleContext the framework context for the bundle.
     */
    @Override
    public void start ( BundleContext bundleContext ) throws Exception {
        bundleContext.registerService( bundleInterface.getName(), bundleImpl, props );
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