package com.dotmarketing.portlets.rules.parameter.comparison;

/**
 * @author Geoff M. Granum
 */
public class NotEqualComparison extends Comparison<Comparable> {

    public NotEqualComparison() {
        super("notEqual");
    }

    @Override
    public boolean perform(Comparable argA, Comparable argB) {
        return argA.compareTo(argB) != 0;
    }
}
 
