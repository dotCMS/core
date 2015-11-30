package com.dotmarketing.portlets.rules.exception;

import com.dotmarketing.portlets.rules.exception.RuleEngineException;

/**
 * @author Geoff M. Granum
 */
public class ComparisonDoesNotExistException extends RuleEngineException {

    private static final long serialVersionUID = 1L;

    public ComparisonDoesNotExistException(String message, String... messageArgs) {
        super(message, messageArgs);
    }

    public ComparisonDoesNotExistException(Throwable cause, String message, String... messageArgs) {
        super(cause, message, messageArgs);
    }
}
 
