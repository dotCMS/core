package com.dotcms.content.elasticsearch.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Small utility class use it to set and track the data of a running process
 * <p/>
 * Created by Jonathan Gamba
 */
public class BasicProcessStatus implements Serializable {

    private String ERROR_MESSAGE = "errorMessage";
    private String ERROR = "error";
    private String ACTIVE = "active";
    private String MESSAGE = "message";

    private Boolean active;
    private Boolean error;
    private String errorMessage;
    private String message;

    public BasicProcessStatus () {
        active = true;
        error = false;
    }

    public void setStatusMessage ( String message ) {
        this.message = message;
    }

    public void setError ( String errorMessage ) {
        this.error = true;
        this.errorMessage = errorMessage;
    }

    public String getMessage () {
        return message;
    }

    public Boolean isActive () {
        return active;
    }

    public Boolean getError () {
        return error;
    }

    public String getErrorMessage () {
        return errorMessage;
    }

    public void start () {
        active = true;
    }

    public void stop () {
        active = false;
    }

    public Map getStatusMap () {

        Map<String, Object> statusMap = new HashMap<String, Object>();

        statusMap.put( ACTIVE, isActive() );
        statusMap.put( MESSAGE, getMessage() );
        statusMap.put( ERROR, error );
        if ( error ) {
            statusMap.put( ERROR_MESSAGE, getErrorMessage() );
        }

        return statusMap;
    }

}