package com.dotmarketing.osgi.controller;

import com.dotmarketing.osgi.GenericBundleActivator;

import java.util.Hashtable;

/**
 * Created by Jonathan Gamba
 * Date: 6/18/12
 */
public class Activator extends GenericBundleActivator {

    public Activator () {

        super( ExampleService.class, new ExampleServiceImpl() );

        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put( "Language", "English" );

        setProperties( props );
    }

}