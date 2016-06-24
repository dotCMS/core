package com.dotmarketing.osgi;

import com.dotcms.repackage.org.apache.felix.http.api.ExtHttpService;
import com.dotcms.repackage.org.osgi.framework.BundleActivator;
import com.dotcms.repackage.org.osgi.framework.BundleContext;
import com.dotcms.repackage.org.osgi.framework.ServiceReference;

import java.io.Serializable;

/**
 * Bundle for Publish activation.
 * @author jsanca
 */
public class PublishBundleActivator  extends BaseBundleActivator  {

    public PublishBundleActivator() {
        super();
    }

    protected PublishBundleActivator(final GenericBundleActivator bundleActivator) {
        super(bundleActivator);
    }


    protected final ExtHttpService getExtHttpService (final BundleContext bundleContext) {

        final ServiceReference serviceReference =
                bundleContext.getServiceReference ( ExtHttpService.class.getName() );

        return (null != serviceReference)?
                (ExtHttpService)bundleContext.getService(serviceReference):
                null;
    }

    /**
     * Allow to this bundle elements to be visible and accessible from the host classpath (Current thread class loader)
     *
     * @param context
     * @throws Exception
     */
    protected final void publishBundleServices ( final BundleContext context ) throws Exception {

        this.getBundleActivator().publishBundleServices(context);
    } // publishBundleServices.

    /**
     * Unpublish this bundle elements
     */
    protected void unpublishBundleServices () throws Exception {

        this.getBundleActivator().unpublishBundleServices();
    } // unpublishBundleServices.

} // E:O:F:PublishBundleActivator.
