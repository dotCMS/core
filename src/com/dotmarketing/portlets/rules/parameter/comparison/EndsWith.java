package com.dotmarketing.portlets.rules.parameter.comparison;

import org.hamcrest.Matchers;

/**
 * @author Geoff M. Granum
 */
public class EndsWith extends Comparison<String> {

    public EndsWith() {
        super("endsWith");
    }

    @Override
    public boolean perform(String argA, String argB) {
        return MatcherCheck.verifyThat(argA, Matchers.endsWith(argB));
    }
}
 
