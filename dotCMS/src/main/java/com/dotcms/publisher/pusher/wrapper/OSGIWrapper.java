package com.dotcms.publisher.pusher.wrapper;

import com.dotcms.publishing.PublisherConfig.Operation;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * @author Jonathan Gamba
 *         Date: 5/17/13
 */
public class OSGIWrapper {

    private Operation operation;
    private String jarName;

    @JsonCreator
    public OSGIWrapper (@JsonProperty("jarName") String jarName, @JsonProperty("operation") Operation operation ) {
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