package com.dotmarketing.osgi.actionlet;

import com.dotmarketing.osgi.GenericBundleActivator;
import org.osgi.framework.BundleContext;

public class Activator extends GenericBundleActivator {

    @Override
    public void start ( BundleContext bundleContext ) throws Exception {

        //Registering the test Actionlet
        registerActionlet( bundleContext, new MyActionlet() );
    }

    @Override
    public void stop ( BundleContext bundleContext ) throws Exception {
        unregisterActionlets();
    }

}