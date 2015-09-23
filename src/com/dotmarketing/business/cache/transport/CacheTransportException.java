package com.dotmarketing.business.cache.transport;

/**
 * @author Jonathan Gamba
 *         Date: 8/25/15
 */
public class CacheTransportException extends RuntimeException {

    private static final long serialVersionUID = -8906239702050272454L;
    private String message;

    public CacheTransportException ( String message ) {
        this.message = message;
    }

    public CacheTransportException ( String message, Exception e ) {
        this.message = message;
        super.initCause(e);
    }

    public CacheTransportException ( Exception e ) {
        this.message = e.getMessage();
        super.initCause(e);
    }

    public String getMessage () {
        return message;
    }

}