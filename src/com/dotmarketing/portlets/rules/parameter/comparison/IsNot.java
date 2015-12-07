package com.dotmarketing.portlets.rules.parameter.comparison;

/**
 * @author Geoff M. Granum
 */
public class IsNot extends Comparison<Comparable<Object>> {

    public IsNot() {
        super("isNot");
    }

    @Override
    public boolean perform(Comparable<Object> left, Comparable<Object> right) {
        return !java.util.Objects.equals(left, right);
    }
}
 
