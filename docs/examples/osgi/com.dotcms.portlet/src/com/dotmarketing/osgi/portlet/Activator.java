package com.dotmarketing.osgi.portlet;

import com.dotmarketing.osgi.GenericBundleActivator;
import com.liferay.util.Http;
import org.apache.struts.action.ActionMapping;
import org.osgi.framework.BundleContext;

public class Activator extends GenericBundleActivator {

    @SuppressWarnings ("unchecked")
    public void start ( BundleContext context ) throws Exception {

        //************************************************************
        //********REGISTER THE ACTION REQUIRED BY THE PORTLET*********
        //************************************************************
        // Creating an ActionConfig Instance
        ActionMapping actionConfig = new ActionMapping();

        // Configure the instance
        actionConfig.setPath( "/ext/strutshello/view_hello" );
        actionConfig.setType( "com.dotmarketing.osgi.portlet.HelloWorldAction" );
        //actionConfig.setScope("session");

        //Create and register the forwards for this mapping
        registerActionForward( actionConfig, "portlet.ext.plugins.hello.world.struts", "/strutshelloworld/view.jsp", false );
        registerActionForward( actionConfig, "portlet.ext.plugins.hello.world.struts.max", "/strutshelloworld/view_hello.jsp", false );

        //And finally register the ActionMapping
        registerActionMapping( actionConfig );

        //************************************************************
        //*******************REGISTER THE PORTLETS********************
        //************************************************************
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