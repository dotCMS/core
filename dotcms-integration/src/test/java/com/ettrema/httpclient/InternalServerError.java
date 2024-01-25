package com.ettrema.httpclient;

/**
 *
 * @author mcevoyb
 */
public class InternalServerError extends HttpException {

    private static final long serialVersionUID = 1L;

    public InternalServerError( String href, int result ) {
        super( result, href );
    }
}
