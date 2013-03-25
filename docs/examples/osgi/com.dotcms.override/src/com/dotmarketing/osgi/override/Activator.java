package com.dotmarketing.osgi.override;

import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.portlets.folders.model.Folder;
import org.osgi.framework.BundleContext;

/**
 * Created by Jonathan Gamba
 * Date: 6/18/12
 */
public class Activator extends GenericBundleActivator {

    @SuppressWarnings ("unchecked")
    public void start ( BundleContext context ) throws Exception {

        //Expose bundle elements
        publishBundleServices( context );

        /**
         * Trying to use our custom implementation of this class, after the last method call should be possible
         * if was added the Override-Classes property inside the MANIFEST.MF.
         */
        Folder folder = new Folder();
        folder.getPath();
    }

    public void stop ( BundleContext context ) throws Exception {

        //Unpublish bundle services
        unpublishBundleServices();
    }

}