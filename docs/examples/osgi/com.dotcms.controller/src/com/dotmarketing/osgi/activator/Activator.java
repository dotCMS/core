package com.dotmarketing.osgi.activator;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.springframework.web.servlet.DispatcherServlet;

import com.dotmarketing.filters.CMSFilter;

/**
 * Created by Jonathan Gamba
 * Date: 6/18/12
 */
public class Activator implements BundleActivator {

    @SuppressWarnings ("unchecked")
    public void start ( BundleContext context ) throws Exception {

        ServiceReference sRef = context.getServiceReference( HttpService.class.getName() );
        if ( sRef != null ) {

            HttpService service = (HttpService) context.getService( sRef );
            try {
                DispatcherServlet ds = new DispatcherServlet();
                ds.setContextConfigLocation( "spring/example-servlet.xml" );
                service.registerServlet( "/spring", ds, null, null );
            } catch ( Exception e ) {
                e.printStackTrace();
            }
            CMSFilter.addExclude("/dynmaic/spring");
        }
    }

    public void stop ( BundleContext context ) throws Exception {
    	CMSFilter.removeExclude("/dynmaic/spring");
    }

}