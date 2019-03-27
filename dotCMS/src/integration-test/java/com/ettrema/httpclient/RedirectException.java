package com.ettrema.httpclient;

/**
 *
 * @author mcevoyb
 */
public class RedirectException extends HttpException {
    private static final long serialVersionUID = 1L;

    public RedirectException( int result, String href ) {
        super( result, href );
    }
}
