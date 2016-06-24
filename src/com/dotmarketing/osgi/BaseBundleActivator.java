package com.dotmarketing.osgi;

import com.dotcms.repackage.org.osgi.framework.BundleActivator;
import com.dotcms.repackage.org.osgi.framework.BundleContext;

import java.io.Serializable;

/**
 * Base Bundle Activator
 * @author jsanca
 */
public class BaseBundleActivator implements BundleActivator, Serializable {

    private final GenericBundleActivator bundleActivator;
    private final boolean isComposite;

    public BaseBundleActivator() {

        this.bundleActivator =
                new ConcreteGenericBundleActivator();
        this.isComposite = false;
    }

    // Only for friends
    protected BaseBundleActivator(final GenericBundleActivator bundleActivator) {

        this.bundleActivator =
                bundleActivator;
        this.isComposite = true;
    }

    // friend access
    final GenericBundleActivator getBundleActivator() {
        return bundleActivator;
    }

    /**
     * Verify and initialize if necessary the required OSGI services to create plugins
     *
     * @param context
     */
    protected final void initializeServices ( final BundleContext context ) throws Exception {

        this.bundleActivator.initializeServices(context);
    }

    /**
     * Utility method to unregister all the possible services and/or tools registered by this activator class.
     * Some how we have to try to clean up anything added on the deploy if this bundle.
     */
    protected void unregisterServices ( BundleContext context ) throws Exception {

        this.bundleActivator.unregisterServices(context);
    }

    @Override
    public void start(final BundleContext bundleContext) throws Exception {

        if (!this.isComposite) {
            this.initializeServices(bundleContext);
        }
    }

    @Override
    public void stop(final BundleContext bundleContext) throws Exception {

        if (!this.isComposite) {
            this.unregisterServices(bundleContext);
        }
    }
} // E:O:F:BaseBundleActivator.
