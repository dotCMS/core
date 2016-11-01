package com.dotmarketing.portlets.rules.parameter.comparison;

/**
 * @author Geoff M. Granum
 */
public class GreaterThanComparison extends Comparison<Comparable> {

    public GreaterThanComparison() {
        super("greaterThan");
    }

    @Override
    public boolean perform(Comparable argA, Comparable argB) {
        return argA.compareTo(argB) > 0;
    }
}
 
