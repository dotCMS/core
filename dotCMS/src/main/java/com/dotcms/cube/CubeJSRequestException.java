package com.dotcms.cube;

/**
 * Throw when a error happened sending a request to a CubeJsServer
 */
public class CubeJSRequestException extends Exception {

    public CubeJSRequestException(Throwable cause) {
        super(cause);
    }

    public CubeJSRequestException(String message) {
        super(message);
    }
}
