package com.dotmarketing.osgi.tuckey;

import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.osgi.GenericBundleActivator;
import org.apache.felix.http.api.ExtHttpService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.tuckey.web.filters.urlrewrite.Condition;
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
        //There are a couple of ways to create tuckey rewrite rules

        //--------------------------------------
        //1) This is the faster and simple way

        //Register a simple tuckey rewrite rule
        addRewriteRule( "^/example/url/forward/$", "/app/helloworld", "forward", "ExampleTuckeyForward" );

        //Register a simple tuckey rewrite rule
        addRewriteRule( "^/example/url/redirect/$", "/app/helloworld", "redirect", "ExampleTuckeyRedirect" );

        //--------------------------------------
        /*
         2) Creating a tuckey Rule,what this rule do can be easily accomplish by using the "addRewriteRule"
         but what make it is different the Condition we added to the rule, that condition specifies that the
         rule is not going to be execute it unless we are using a chrome browser.
          */
        //Creating a tuckey rule
        NormalRule forwardRule = new NormalRule();
        forwardRule.setFrom( "^/example/url/condition/$" );
        forwardRule.setToType( "forward" );
        forwardRule.setTo( "/app/helloworld?browser=chrome" );
        forwardRule.setName( "ExampleTuckeyCondition" );
        //Create a Condition for this rule
        Condition condition = new Condition();
        condition.setName( "user-agent" );
        condition.setValue( "Chrome/*.*" );
        forwardRule.addCondition( condition );

        //Register the tuckey rewrite rule
        addRewriteRule( forwardRule );
    }

    public void stop ( BundleContext context ) throws Exception {

        //Unregister all the bundle services
        unregisterServices( context );
    }

}