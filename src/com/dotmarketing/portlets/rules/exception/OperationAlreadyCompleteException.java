package com.dotmarketing.portlets.rules.exception;

/**
 * @author Geoff M. Granum
 */
public class OperationAlreadyCompleteException extends RuleEngineException {

    private static final long serialVersionUID = 1L;

    public OperationAlreadyCompleteException(String message, String... messageArgs) {
        super(message, messageArgs);
    }
}
 
