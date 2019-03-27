package com.dotmarketing.portlets.rules.parameter.comparison;

import java.util.regex.Pattern;

/**
 * @author Geoff M. Granum
 */
public class RegexComparison extends Comparison<String> {

    public RegexComparison() {
        super("regex");
    }

    @Override
    public boolean perform(String argA, String argB) {
        return Pattern.matches(argB, argA);
    }
}
 
