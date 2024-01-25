package com.ettrema.httpclient;

/**
 *
 * @author mcevoyb
 */
public class GenericHttpException extends HttpException {
    private static final long serialVersionUID = 1L;

    public GenericHttpException( int result, String href ) {
        super( result, href );
    }
    
    public GenericHttpException( String href, Throwable ex ) {
        super(href, ex);
    }    
}
