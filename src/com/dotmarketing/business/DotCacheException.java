/**
 *
 */
package com.dotmarketing.business;

/**
 * @author Jason Tesser
 * @since 1.6
 */
public class DotCacheException extends Exception {

    private static final long serialVersionUID = 4341568856831804802L;
    private String message;

    public DotCacheException ( String message ) {
        this.message = message;
    }

    public DotCacheException ( String message, Exception e ) {
        this.message = message;
        super.initCause(e);
    }

    public DotCacheException ( Exception e ) {
        this.message = e.getMessage();
        super.initCause(e);
    }

    public String getMessage () {
        return message;
    }

}