package com.dotmarketing.portlets.rules.exception;

/**
 * @author Geoff M. Granum
 */
public class InvalidActionInstanceException extends RuleEngineException {

    private static final long serialVersionUID = 1L;

    public InvalidActionInstanceException(String message, String... messageArgs) {
        this(null, message, messageArgs);
    }

    public InvalidActionInstanceException(Throwable cause, String message, String... messageArgs) {
        super(cause, message, messageArgs);
    }


}
 
