package com.dotmarketing.osgi;

import com.dotcms.rest.config.DotBundleListener;
import com.dotmarketing.util.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Main OSGi host activator for dotCMS
 */
public class HostActivator implements BundleActivator {

    private BundleContext m_context = null;
    private static HostActivator instance;
    private DotBundleListener bundleListener = null;

    private HostActivator() {
        Logger.info(this, "HostActivator instance created");
    }

    public synchronized static HostActivator instance() {
        if (instance == null) {
            instance = new HostActivator();
        }
        return instance;
    }

    public void start(BundleContext context) {
        Logger.info(this, "HostActivator.start() - Begin initialization");
        m_context = context;
        
        try {
            // Log all installed bundles
            Bundle[] bundles = context.getBundles();
            Logger.info(this, "Currently installed bundles: " + bundles.length);
            for (Bundle bundle : bundles) {
                Logger.info(this, String.format("Bundle: %s [ID: %d, State: %d]", 
                        bundle.getSymbolicName(), 
                        bundle.getBundleId(),
                        bundle.getState()));
            }
        } catch (Exception e) {
            Logger.error(this, "Error listing installed bundles", e);
        }
        
        // Initialize the bundle listener to automatically clean up REST resources
        try {
            bundleListener = DotBundleListener.getInstance();
            bundleListener.init(context);
            
            if (bundleListener.isInitialized()) {
                Logger.info(this, "DotBundleListener successfully registered with OSGi framework");
            } else {
                Logger.error(this, "DotBundleListener registration FAILED");
            }
        } catch (Exception e) {
            Logger.error(this, "Error registering DotBundleListener", e);
        }
        
        Logger.info(this, "HostActivator.start() - Initialization complete");
    }

    public void stop(BundleContext context) {
        Logger.info(this, "HostActivator.stop() - Begin shutdown");
        
        // Clean up the bundle listener
        try {
            if (bundleListener != null) {
                bundleListener.destroy();
                Logger.info(this, "DotBundleListener unregistered successfully");
            }
        } catch (Exception e) {
            Logger.error(this, "Error unregistering DotBundleListener", e);
        }
        
        m_context = null;
        Logger.info(this, "HostActivator.stop() - Shutdown complete");
    }

    public BundleContext getBundleContext() {
        return m_context;
    }

    public Bundle[] getBundles() {
        if (m_context != null)
            return m_context.getBundles();
        return null;
    }
}
