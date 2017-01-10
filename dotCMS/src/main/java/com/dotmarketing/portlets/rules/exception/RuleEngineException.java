package com.dotmarketing.portlets.rules.exception;

/**
 * @author Geoff M. Granum
 */
public class RuleEngineException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RuleEngineException(String message, String... messageArgs) {
        this(null, message, messageArgs);
    }

    public RuleEngineException(Throwable cause, String message, String... messageArgs) {
        super(String.format(message == null  ? "" : message, (String[])messageArgs), cause);
    }


}
 
