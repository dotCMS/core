package com.dotmarketing.portlets.rules.exception;

/**
 * @author Geoff M. Granum
 */
public class RuleEvaluationFailedException extends RuleEngineException {

    private static final long serialVersionUID = 1L;

    public RuleEvaluationFailedException(String message, String... messageArgs) {
        super(message, messageArgs);
    }

    public RuleEvaluationFailedException(Throwable cause, String message, String... messageArgs) {
        super(cause, message, messageArgs);
    }
}
 
