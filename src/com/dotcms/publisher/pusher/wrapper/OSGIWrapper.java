package com.dotcms.publisher.pusher.wrapper;

import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;

/**
 * @author Jonathan Gamba
 *         Date: 5/17/13
 */
public class OSGIWrapper {

    private Operation operation;
    private String jarName;

    public OSGIWrapper ( String jarName, Operation operation ) {
        this.operation = operation;
        this.jarName = jarName;
    }

    public Operation getOperation () {
        return operation;
    }

    public void setOperation ( Operation operation ) {
        this.operation = operation;
    }

    public String getJarName () {
        return jarName;
    }

    public void setJarName ( String jarName ) {
        this.jarName = jarName;
    }

}