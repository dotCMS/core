package com.dotmarketing.portlets.rules.parameter.comparison;

import java.util.List;
import org.hamcrest.Matchers;

/**
 * @author Geoff M. Granum
 */
public class IsOneOf<T extends Comparable> extends Comparison<T> {

    private final List<T> items;

    public IsOneOf(List<T> these) {
        super("isOneOf");
        this.items = these;
    }

    @Override
    public boolean perform(T argA) {
        return MatcherCheck.verifyThat(argA, Matchers.isOneOf(items));
    }
}
 
