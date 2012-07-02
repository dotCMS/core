package com.dotmarketing.osgi.servlet;

import com.dotmarketing.osgi.service.HelloWorld;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

    private ServiceTracker helloWorldServiceTracker;

    @SuppressWarnings ("unchecked")
    public void start ( BundleContext context ) throws Exception {

        //Create new ServiceTracker for HelloWorldService via HelloWorld interface
        helloWorldServiceTracker = new ServiceTracker( context, HelloWorld.class.getName(), null );

        ServiceReference sRef = context.getServiceReference( HttpService.class.getName() );
        if ( sRef != null ) {

            helloWorldServiceTracker.addingService( sRef );
            HttpService service = (HttpService) context.getService( sRef );
            try {
                service.registerServlet( "/helloworld", new HelloWorldServlet( helloWorldServiceTracker ), null, null );
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }

        // open service tracker to start tracking
        helloWorldServiceTracker.open();
    }

    public void stop ( BundleContext context ) throws Exception {

        // close service tracker to stop tracking
        helloWorldServiceTracker.close();
    }

}