package com.dotmarketing.portlets.rules.parameter.comparison;

/**
 * @author Geoff M. Granum
 */
public class GreaterThanComparison extends Comparison<Comparable<Object>> {

    public GreaterThanComparison() {
        super("greater_than");
    }

    @Override
    public boolean perform(Comparable<Object> argA, Comparable<Object> argB) {
        return argA.compareTo(argB) > 0;
    }
}
 
