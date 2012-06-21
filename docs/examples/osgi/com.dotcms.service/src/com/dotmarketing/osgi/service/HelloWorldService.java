package com.dotmarketing.osgi.service;

/**
 * Created by Jonathan Gamba
 */
public class HelloWorldService implements com.dotmarketing.osgi.service.manual.HelloWorld {

    @Override
    public String hello () {
        return "Hello word!!";
    }

}