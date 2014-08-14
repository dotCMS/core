package com.dotmarketing.osgi.custom.dwr.osgi;

import com.dotcms.repackage.org.apache.felix.http.api.ExtHttpService;
import com.dotcms.repackage.org.directwebremoting.servlet.DwrServlet;
import com.dotcms.repackage.org.osgi.framework.BundleContext;
import com.dotcms.repackage.org.osgi.framework.ServiceReference;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.osgi.GenericBundleActivator;

/**
 * @author Nathan Keiter
 *         Date: 12/05/13
 */
public class Activator extends GenericBundleActivator {

    private DwrServlet dwrServlet;
    private ExtHttpService extHttpService;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void start ( BundleContext context ) throws Exception {

        //Initializing services...
        initializeServices( context );

        //Service reference to ExtHttpService that allows us to register servlets and filters
        ServiceReference serviceReference = context.getServiceReference( ExtHttpService.class.getName() );

        if ( serviceReference != null ) {

            //Publish bundle services
            publishBundleServices( context );

            //Load http service extension object from service reference
            extHttpService = (ExtHttpService) context.getService( serviceReference );

            //Create our DwrServlet instance
            dwrServlet = new DwrServlet();

            //Register our DwrServlet
            extHttpService.registerServlet( "/custom_dwr", dwrServlet, null, null );
        }

        //Add servlet path to CMS exclusion list
        CMSFilter.addExclude( "/app/custom_dwr" );
    }

    public void stop ( BundleContext context ) throws Exception {

        //Unregister all the bundle services
        unregisterServices( context );
    }

}