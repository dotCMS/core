package com.dotmarketing.osgi.service;

import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.osgi.service.manual.HelloWorldService;

import java.util.Hashtable;

/**
 * Created by Jonathan Gamba
 * Date: 6/18/12
 */
public class Activator extends GenericBundleActivator {

    public Activator () {

        super( com.dotmarketing.osgi.service.manual.HelloWorld.class, new HelloWorldService() );

        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( "Language", "English" );

        setProperties( props );
    }

}