package com.dotmarketing.portlets.rules.parameter.comparison;

/**
 * @author Geoff M. Granum
 */
public class GreaterThanOrEqualComparison extends Comparison<Comparable<Object>> {

    public GreaterThanOrEqualComparison() {
        super("greater_than_or_equal");
    }

    @Override
    public boolean perform(Comparable<Object> argA, Comparable<Object> argB) {
        return argA.compareTo(argB) >= 0;
    }
}
 
