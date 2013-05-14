package com.dotcms.publisher.pusher.wrapper;

import com.dotcms.publisher.pusher.PushPublisherConfig;

/**
 * @author Jonathan Gamba
 *         Date: 5/14/13
 */
public class OSGIWrapper {

    private PushPublisherConfig.Operation operation;
    private String fileName;

    public OSGIWrapper ( String fileName ) {
        this.fileName = fileName;
    }

    public String getFileName () {
        return fileName;
    }

    public void setFileName ( String fileName ) {
        this.fileName = fileName;
    }

    public PushPublisherConfig.Operation getOperation () {
        return operation;
    }

    public void setOperation ( PushPublisherConfig.Operation operation ) {
        this.operation = operation;
    }

}