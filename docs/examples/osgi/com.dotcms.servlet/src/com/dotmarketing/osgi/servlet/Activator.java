package com.dotmarketing.osgi.servlet;

import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.osgi.service.HelloWorld;
import org.apache.felix.http.api.ExtHttpService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Activator extends GenericBundleActivator {

    private ServiceTracker helloWorldServiceTracker;

    @SuppressWarnings ("unchecked")
    public void start ( BundleContext context ) throws Exception {

        //Initializing services...
        initializeServices( context );

        //Create new ServiceTracker for HelloWorldService via HelloWorld interface
        helloWorldServiceTracker = new ServiceTracker( context, HelloWorld.class.getName(), null );

        //Service reference to ExtHttpService that will allows to register servlets and filters
        ServiceReference sRef = context.getServiceReference( ExtHttpService.class.getName() );
        if ( sRef != null ) {

            helloWorldServiceTracker.addingService( sRef );
            ExtHttpService service = (ExtHttpService) context.getService( sRef );
            try {
                //Registering a simple test servlet
                service.registerServlet( "/helloworld", new HelloWorldServlet( helloWorldServiceTracker ), null, null );

                //Registering a simple test filter
                service.registerFilter( new TestFilter( "testFilter" ), "/helloworld/.*", null, 100, null );
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }

        CMSFilter.addExclude( "/app/helloworld" );

        // open service tracker to start tracking
        helloWorldServiceTracker.open();
    }

    public void stop ( BundleContext context ) throws Exception {

        CMSFilter.removeExclude( "/app/helloworld" );

        // close service tracker to stop tracking
        helloWorldServiceTracker.close();
    }

}