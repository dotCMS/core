package com.dotmarketing.portlets.rules.exception;

import com.dotmarketing.portlets.rules.exception.RuleEngineException;

/**
 * @author Geoff M. Granum
 */
public class ComparisonExistsException extends RuleEngineException {

    private static final long serialVersionUID = 1L;

    public ComparisonExistsException(String message, String... messageArgs) {
        super(message, messageArgs);
    }

    public ComparisonExistsException(Throwable cause, String message, String... messageArgs) {
        super(cause, message, messageArgs);
    }
}
 
