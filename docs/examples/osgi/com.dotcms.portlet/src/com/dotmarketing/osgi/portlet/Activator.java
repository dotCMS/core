package com.dotmarketing.osgi.portlet;

import com.dotmarketing.osgi.GenericBundleActivator;
import org.apache.struts.action.ActionMapping;
import org.osgi.framework.BundleContext;

public class Activator extends GenericBundleActivator {

    @SuppressWarnings ("unchecked")
    public void start ( BundleContext context ) throws Exception {

        //************************************************************
        //*****REGISTER THE ACTION REQUIRED BY THE STRUTS PORTLET*****
        //************************************************************
        //Creating an ActionMapping Instance
        ActionMapping actionMapping = new ActionMapping();

        //Configure the instance
        actionMapping.setPath( "/ext/strutshello/view_hello" );
        actionMapping.setType( "com.dotmarketing.osgi.portlet.HelloWorldAction" );

        //Create and register the forwards for this mapping
        registerActionForward( context, actionMapping, "portlet.ext.plugins.hello.world.struts", "/ext/strutshelloworld/view.jsp", false );
        registerActionForward( context, actionMapping, "portlet.ext.plugins.hello.world.struts.max", "/ext/strutshelloworld/view_hello.jsp", false );

        //And finally register the ActionMapping
        registerActionMapping( actionMapping );

        //************************************************************
        //*******************REGISTER THE PORTLETS********************
        //************************************************************
        //Register our portlets
        String[] xmls = new String[]{"conf/portlet.xml", "conf/liferay-portlet.xml"};
        registerPortlets( context, xmls );
    }

    public void stop ( BundleContext context ) throws Exception {

        //Unregister all the bundle services
        unregisterServices( context );
    }

}