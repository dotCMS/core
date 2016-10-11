package com.ettrema.httpclient;

/**
 *
 * @author mcevoyb
 */
public class MethodNotAllowedException extends HttpException {
    private static final long serialVersionUID = 1L;

    public MethodNotAllowedException( int result, String href ) {
        super( result, href );
    }
}
