package com.dotmarketing.portlets.rules.parameter.comparison;

import org.hamcrest.Matchers;

/**
 * @author Geoff M. Granum
 */
public class Contains extends Comparison<String> {

    public Contains() {
        super("contains");
    }

    @Override
    public boolean perform(String argA, String argB) {
        return MatcherCheck.verifyThat(argA, Matchers.containsString(argB));
    }
}
 
