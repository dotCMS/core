package com.dotmarketing.osgi;

import com.dotcms.repackage.org.osgi.framework.BundleContext;

/**
 * This class will be used by other minimal implementations
 * @author jsanca
 */
class ConcreteGenericBundleActivator extends GenericBundleActivator {


    public ConcreteGenericBundleActivator () {

    }


    @Override
    public void start(BundleContext bundleContext) throws Exception {

        // does not need it
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {

        // does not need it
    }
}
