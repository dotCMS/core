package com.dotmarketing.osgi.spring;

import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.osgi.GenericBundleActivator;
import org.apache.felix.http.api.ExtHttpService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Created by Jonathan Gamba
 * Date: 6/18/12
 */
public class Activator extends GenericBundleActivator {

    @SuppressWarnings ("unchecked")
    public void start ( BundleContext context ) throws Exception {

        //Initializing services...
        initializeServices( context );

        //Service reference to ExtHttpService that will allows to register servlets and filters
        ServiceReference sRef = context.getServiceReference( ExtHttpService.class.getName() );
        if ( sRef != null ) {

            //Publish bundle services
            publishBundleServices( context );

            ExtHttpService service = (ExtHttpService) context.getService( sRef );
            try {
                DispatcherServlet ds = new DispatcherServlet();
                ds.setContextConfigLocation( "spring/example-servlet.xml" );
                service.registerServlet( "/spring", ds, null, null );
            } catch ( Exception e ) {
                e.printStackTrace();
            }
            CMSFilter.addExclude( "/dynamic/spring" );
        }
    }

    public void stop ( BundleContext context ) throws Exception {

        CMSFilter.removeExclude( "/dynamic/spring" );

        //Unpublish bundle services
        unpublishBundleServices();
    }

}