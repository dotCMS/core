package com.dotmarketing.osgi.servlet;

import com.dotmarketing.osgi.service.manual.HelloWorld;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;


public class Activator implements BundleActivator {


    private ServiceTracker httpServiceTracker;
    private ServiceTracker helloWorldServiceTracker;

    @SuppressWarnings ("unchecked")
    public void start ( BundleContext context ) throws Exception {

        // create new ServiceTracker for HelloWorldService via HelloWorld interface
        helloWorldServiceTracker = new ServiceTracker( context, HelloWorld.class.getName(), null );

        // create new ServiceTracker for HttpService
        httpServiceTracker = new ServiceTracker( context, HttpService.class.getName(), null ) {

            public Object addingService ( ServiceReference reference ) {

                HttpService httpService = (HttpService) super.addingService( reference );

                try {

                    httpService.registerServlet( "/helloworld", new HelloWorldServlet( helloWorldServiceTracker ), null, null );
                } catch ( Exception e ) {
                    e.printStackTrace();
                }

                return httpService;
            }

            public void removedService ( ServiceReference reference, Object service ) {

                ((HttpService) service).unregister( "/helloworld" );
                super.removedService( reference, service );
            }

        };

        // open service tracker to start tracking
        helloWorldServiceTracker.open();
        httpServiceTracker.open();
    }

    public void stop ( BundleContext context ) throws Exception {

        // open service tracker to stop tracking
        httpServiceTracker.close();
        helloWorldServiceTracker.close();
    }

}