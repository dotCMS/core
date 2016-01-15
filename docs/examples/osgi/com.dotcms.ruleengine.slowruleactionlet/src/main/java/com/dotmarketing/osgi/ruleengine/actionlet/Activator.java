package com.dotmarketing.osgi.ruleengine.actionlet;

import com.dotcms.repackage.org.osgi.framework.BundleContext;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.osgi.ruleengine.actionlet.SlowRuleActionlet;

/**
 * This plugin helps to test the slow rule engine message
 * when a rule engine execution takes too much time
 * @author oswaldogallango
 *
 */
public class Activator extends GenericBundleActivator {
 

    @Override
    public void start ( BundleContext bundleContext ) throws Exception {

        //Initializing services...
        initializeServices( bundleContext );

        //Registering the Conditionlet
        registerRuleActionlet( bundleContext, new SlowRuleActionlet() );
    }

    @Override
    public void stop ( BundleContext bundleContext ) throws Exception {
        unregisterServices(bundleContext);
    }

}