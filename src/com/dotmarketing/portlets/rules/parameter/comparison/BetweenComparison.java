package com.dotmarketing.portlets.rules.parameter.comparison;

/**
 * @author Geoff M. Granum
 */
public class BetweenComparison extends Comparison<Comparable<Object>> {

    public BetweenComparison() {
        super("between");
    }

    @Override
    public boolean perform(Comparable<Object> argA, Comparable<Object> argB, Comparable<Object> argC) {
        int left = argA.compareTo(argB);
        int right = argB.compareTo(argC);
        int spread = argA.compareTo(argC);
        return left <= right;
    }
}
 
