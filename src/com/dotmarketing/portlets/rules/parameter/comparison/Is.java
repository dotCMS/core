package com.dotmarketing.portlets.rules.parameter.comparison;

import static org.hamcrest.Matchers.equalTo;

/**
 * @author Geoff M. Granum
 */
public class Is extends Comparison<Comparable> {

    public Is() {
        super("is");
    }

    @Override
    public boolean perform(Comparable expect, Comparable right) {
        return MatcherCheck.verifyThat(expect, equalTo(right));
    }
}
 
