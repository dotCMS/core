package com.dotmarketing.osgi.servlet;

import com.dotmarketing.osgi.service.HelloWorld;
import org.apache.felix.http.api.ExtHttpService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.Servlet;
import java.util.Hashtable;


public class Activator implements BundleActivator {


    private ServiceTracker extHttpServiceTracker;
    private ServiceTracker httpServiceTracker;
    private ServiceTracker helloWorldServiceTracker;

    private ServiceRegistration registration;

    @SuppressWarnings ("unchecked")
    public void start ( BundleContext context ) throws Exception {

        // create new ServiceTracker for HelloWorldService via HelloWorld interface
        helloWorldServiceTracker = new ServiceTracker( context, HelloWorld.class.getName(), null );
        
        //1) // create new ServiceTracker for HttpService
        ServiceReference sRef = context.getServiceReference( HttpService.class.getName() );
        //ServiceReference sRef2 = context.getServiceReference( ExtHttpService.class.getName() );
        if ( sRef != null ) {

            helloWorldServiceTracker.addingService( sRef );
            HttpService service = (HttpService) context.getService( sRef );
            try {
            	DispatcherServlet ds = new DispatcherServlet();
            	ds.setContextConfigLocation("classpath:/spring/dispatch-servlet.xml");
                service.registerServlet( "/spring", ds, null, null );
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }

        /*//2) // create new ServiceTracker for HttpService
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

        // create new ServiceTracker for HttpService
        extHttpServiceTracker = new ServiceTracker( context, ExtHttpService.class.getName(), null ) {

            public Object addingService ( ServiceReference reference ) {

                ExtHttpService extHttpService = (ExtHttpService) super.addingService( reference );

                try {

                    extHttpService.registerServlet( "/helloworld", new HelloWorldServlet( helloWorldServiceTracker ), null, null );
                } catch ( Exception e ) {
                    e.printStackTrace();
                }

                return extHttpService;
            }

            public void removedService ( ServiceReference reference, Object service ) {

                ((ExtHttpService) service).unregister( "/helloworld" );
                super.removedService( reference, service );
            }

        };*/

        //3)
        /*Hashtable props = new Hashtable();
        props.put( "alias", "/helloworld" );
        props.put( "init.message", "Hello World!" );

        this.registration = context.registerService( Servlet.class.getName(), new HelloWorldServlet( helloWorldServiceTracker ), props );*/

        // open service tracker to start tracking
        helloWorldServiceTracker.open();
        //httpServiceTracker.open();
        //extHttpServiceTracker.open();
    }

    public void stop ( BundleContext context ) throws Exception {

        // open service tracker to stop tracking
        //httpServiceTracker.close();
        //extHttpServiceTracker.close();

        /*ServiceReference sRef = context.getServiceReference( HttpService.class.getName() );
        HttpService service = (HttpService) context.getService( sRef );
        helloWorldServiceTracker.removedService( sRef, service );*/
        helloWorldServiceTracker.close();
    }

}