package com.dotmarketing.osgi.viewtools;

import com.dotcms.repackage.org.osgi.framework.BundleContext;
import com.dotmarketing.osgi.GenericBundleActivator;

public class Activator extends GenericBundleActivator {

    @Override
    public void start ( BundleContext bundleContext ) throws Exception {

        //Initializing services...
        initializeServices( bundleContext );

        //Registering the ViewTool service
        registerViewToolService( bundleContext, new MyToolInfo() );
    }

    @Override
    public void stop ( BundleContext bundleContext ) throws Exception {
        unregisterViewToolServices();
    }

}