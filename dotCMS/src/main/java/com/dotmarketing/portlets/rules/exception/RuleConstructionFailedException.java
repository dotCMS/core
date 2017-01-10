package com.dotmarketing.portlets.rules.exception;

/**
 * @author Geoff M. Granum
 */
public class RuleConstructionFailedException extends RuleEngineException {

    private static final long serialVersionUID = 1L;

    public RuleConstructionFailedException(String message, String... messageArgs) {
        super(message, messageArgs);
    }

    public RuleConstructionFailedException(Throwable cause, String message, String... messageArgs) {
        super(cause, message, messageArgs);
    }
}
 
