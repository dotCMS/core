package com.ettrema.httpclient;

/**
 *
 * @author mcevoyb
 */
public abstract class HttpException extends Exception {

    private static final long serialVersionUID = 1L;
    private final int result;
    private final String href;

    public HttpException(int result, String href) {
        super("http error: " + result + " - " + href);
        this.result = result;
        this.href = href;
    }

    public HttpException(String href, Throwable cause) {
        super(href, cause);
        this.href = href;
        this.result = 0;
    }
    
    

    public int getResult() {
        return result;
    }

    public String getHref() {
        return href;
    }
}
