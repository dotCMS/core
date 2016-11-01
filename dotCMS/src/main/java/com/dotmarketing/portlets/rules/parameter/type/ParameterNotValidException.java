package com.dotmarketing.portlets.rules.parameter.type;

import com.dotmarketing.portlets.rules.exception.RuleEngineException;

/**
 * @author Geoff M. Granum
 */
public class ParameterNotValidException extends RuleEngineException {

    private static final long serialVersionUID = 1L;

    public ParameterNotValidException(String message, String... messageArgs) {
        super(message, messageArgs);
    }

    public ParameterNotValidException(Throwable cause, String message, String... messageArgs) {
        super(cause, message, messageArgs);
    }
}
 
