package com.dotmarketing.osgi.tuckey;

import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.osgi.GenericBundleActivator;
import org.apache.felix.http.api.ExtHttpService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.tuckey.web.filters.urlrewrite.NormalRule;

public class Activator extends GenericBundleActivator {

    @SuppressWarnings ("unchecked")
    public void start ( BundleContext context ) throws Exception {

        //Initializing services...
        initializeServices( context );

        //REGISTERING A SIMPLE SERVLET IN ORDER TO TEST THE TUCKEY REWRITE TOOLS

        //Service reference to ExtHttpService that will allows to register servlets and filters
        ServiceReference sRef = context.getServiceReference( ExtHttpService.class.getName() );
        if ( sRef != null ) {

            ExtHttpService httpService = (ExtHttpService) context.getService( sRef );
            try {
                //Registering a simple test servlet
                HelloWorldServlet simpleServlet = new HelloWorldServlet();
                httpService.registerServlet( "/helloworld", simpleServlet, null, null );

                CMSFilter.addExclude( "/app/helloworld" );
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }

        //ADDING SOME URL REWRITE RULES

        //--------------------------------------
        //Creating a tuckey rule
        NormalRule forwardRule = new NormalRule();
        forwardRule.setFrom( "^/example/url/forward/$" );
        forwardRule.setToType( "forward" );
        forwardRule.setTo( "/app/helloworld" );
        forwardRule.setName( "ExampleTuckeyForward" );

        //Register the tuckey rewrite rule
        addRewriteRule( forwardRule );

        //--------------------------------------
        //Creating a tuckey rule
        NormalRule redirectRule = new NormalRule();
        redirectRule.setFrom( "^/example/url/redirect/$" );
        redirectRule.setToType( "redirect" );
        redirectRule.setTo( "/app/helloworld" );
        redirectRule.setName( "ExampleTuckeyRedirect" );

        //Register the tuckey rewrite rule
        addRewriteRule( redirectRule );
    }

    public void stop ( BundleContext context ) throws Exception {

        //Unregister all the bundle services
        unregisterServices( context );
    }

}