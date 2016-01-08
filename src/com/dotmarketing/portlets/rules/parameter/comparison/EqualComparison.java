package com.dotmarketing.portlets.rules.parameter.comparison;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author Geoff M. Granum
 */
public class EqualComparison extends Comparison<Comparable> {

    public EqualComparison() {
        super("equal");
    }

    @Override
    public boolean perform(Comparable argA, Comparable argB) {
        return argA.compareTo(argB) == 0;
    }
}
 
