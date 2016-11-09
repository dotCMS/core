package com.dotmarketing.portlets.rules.exception;


/**
 * @author Geoff M. Granum
 */
public class ComparisonNotPresentException extends RuleEngineException {

    private static final long serialVersionUID = 1L;
    private final String comparisonId;

    public ComparisonNotPresentException(String comparisonId) {
        super("Comparison not present: " + comparisonId);
        this.comparisonId = comparisonId;
    }

}
 
