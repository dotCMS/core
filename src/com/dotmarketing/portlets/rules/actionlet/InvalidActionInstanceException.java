package com.dotmarketing.portlets.rules.actionlet;

/**
 * @author Geoff M. Granum
 */
public class InvalidActionInstanceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidActionInstanceException(String message, String... messageArgs) {
        this(null, message, messageArgs);
    }

    public InvalidActionInstanceException(Throwable cause, String message, String... messageArgs) {
        super(String.format(message == null  ? "" : message, (String[])messageArgs), cause);
    }


}
 
