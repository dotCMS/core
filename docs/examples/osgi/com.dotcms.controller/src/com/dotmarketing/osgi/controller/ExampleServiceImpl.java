package com.dotmarketing.osgi.controller;

/**
 * Created by Jonathan Gamba
 * Date: 6/18/12
 */
public class ExampleServiceImpl implements ExampleService {

    @Override
    public String getCustomServiceCall () {

        return "Called getCustomServiceCall method from TestServiceImpl";
    }

}