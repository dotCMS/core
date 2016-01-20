package com.dotmarketing.portlets.rules.parameter.comparison;

/**
 * @author Geoff M. Granum
 */
public class BetweenComparison extends Comparison<Comparable> {

    public BetweenComparison() {
        super("between", 2);
    }

    @Override
    public boolean perform(Comparable argA, Comparable argB, Comparable argC) {
        int left = argA.compareTo(argB);
        int right = argA.compareTo(argC);
        return left >= right;
    }
}
 
