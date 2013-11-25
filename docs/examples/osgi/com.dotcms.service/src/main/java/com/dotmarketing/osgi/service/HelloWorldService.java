package com.dotmarketing.osgi.service;

/**
 * Created by Jonathan Gamba
 */
public class HelloWorldService implements HelloWorld {

    @Override
    public String hello () {
        return "Hello word!!";
    }

}