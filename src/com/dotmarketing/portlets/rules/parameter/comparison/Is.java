package com.dotmarketing.portlets.rules.parameter.comparison;

import static org.hamcrest.Matchers.equalTo;

/**
 * @author Geoff M. Granum
 */
public class Is extends Comparison<Comparable<Object>> {

    public Is() {
        super("is");
    }

    @Override
    public boolean perform(Comparable<Object> left, Comparable<Object> right) {
        return MatcherCheck.verifyThat(left, equalTo(right));
    }
}
 
