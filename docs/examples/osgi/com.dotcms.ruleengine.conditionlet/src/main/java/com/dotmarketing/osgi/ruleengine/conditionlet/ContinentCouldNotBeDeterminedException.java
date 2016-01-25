package com.dotmarketing.osgi.ruleengine.conditionlet;

import com.dotmarketing.portlets.rules.exception.RuleEngineException;

/**
 * @author Geoff M. Granum
 */
public class ContinentCouldNotBeDeterminedException extends RuleEngineException{

    private static final long serialVersionUID = 1L;

    public ContinentCouldNotBeDeterminedException(String country) {
        super("Could not determine continent from country code '%s'", country );
    }
}
 
