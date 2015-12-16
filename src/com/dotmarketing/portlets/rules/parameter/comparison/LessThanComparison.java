package com.dotmarketing.portlets.rules.parameter.comparison;

/**
 * @author Geoff M. Granum
 */
public class LessThanComparison extends Comparison<Comparable<Object>> {

    public LessThanComparison() {
        super("less_than");
    }

    @Override
    public boolean perform(Comparable<Object> argA, Comparable<Object> argB) {
        return argA.compareTo(argB) < 0;
    }
}
 
