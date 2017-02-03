package com.dotmarketing.portlets.rules.exception;

/**
 * @author Geoff M. Granum
 */
public class ComparisonNotSupportedException extends RuleEngineException {

    private static final long serialVersionUID = 1L;

    public ComparisonNotSupportedException(String message, String... messageArgs) {
        super(message, messageArgs);
    }
}
 
