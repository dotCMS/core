package com.dotmarketing.osgi.portlet;

import com.dotmarketing.osgi.GenericBundleActivator;
import com.liferay.portal.model.Portlet;
import com.liferay.util.Http;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.config.ForwardConfig;
import org.osgi.framework.BundleContext;

import java.util.List;

public class Activator extends GenericBundleActivator {

    List<Portlet> portlets;

    @SuppressWarnings ("unchecked")
    public void start ( BundleContext context ) throws Exception {

        // Creating an ActionConfig Instance
        ActionMapping actionConfig = new ActionMapping();

        // Configure the instance
        actionConfig.setPath( "/ext/strutshello/view_hello" );
        actionConfig.setType( "com.dotmarketing.osgi.portlet.HelloWorldAction" );
        //actionConfig.setScope("session");

        // Creating an ForwardConfig Instance
        ForwardConfig forwardConfig = new ActionForward( "portlet.ext.plugins.hello.world.struts", "/plugins/hello.world/strutshelloworld/view.jsp", false );
        // Adding the ForwardConfig to the ActionConfig
        actionConfig.addForwardConfig( forwardConfig );

        // Creating another ForwardConfig Instance
        forwardConfig = new ActionForward( "portlet.ext.plugins.hello.world.struts.max", "/plugins/hello.world/strutshelloworld/view_hello.jsp", false );
        // Adding the ForwardConfig to the ActionConfig
        actionConfig.addForwardConfig( forwardConfig );

        //And finally register the ActionMapping
        registerActionMapping( actionConfig );

        //Register our portlets
        String[] xmls = new String[]{Http.URLtoString( context.getBundle().getResource( "conf/portlet.xml" ) ),
                Http.URLtoString( context.getBundle().getResource( "conf/liferay-portlet.xml" ) )};
        registerPortlets( xmls );
    }

    public void stop ( BundleContext context ) throws Exception {

        //Unregister all the bundle services
        unregisterServices();
    }

}