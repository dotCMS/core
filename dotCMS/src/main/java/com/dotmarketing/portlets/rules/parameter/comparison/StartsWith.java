package com.dotmarketing.portlets.rules.parameter.comparison;

import org.hamcrest.Matchers;

/**
 * @author Geoff M. Granum
 */
public class StartsWith extends Comparison<String> {

    public StartsWith() {
        super("startsWith");
    }

    @Override
    public boolean perform(String argA, String argB) {
        return MatcherCheck.verifyThat(argA, Matchers.startsWith(argB));
    }
}
 
