package com.dotcms.rendering.js;

/**
 * Just a generic exception for the JS rendering
 */
public class JsException extends Exception {
    public JsException() {
    }

    public JsException(String message) {
        super(message);
    }

    public JsException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsException(Throwable cause) {
        super(cause);
    }

    public JsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
