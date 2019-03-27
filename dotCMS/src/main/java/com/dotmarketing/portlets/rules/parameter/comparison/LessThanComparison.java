package com.dotmarketing.portlets.rules.parameter.comparison;

/**
 * @author Geoff M. Granum
 */
public class LessThanComparison extends Comparison<Comparable> {

    public LessThanComparison() {
        super("lessThan");
    }

    @Override
    public boolean perform(Comparable argA, Comparable argB) {
        return argA.compareTo(argB) < 0;
    }
}
 
