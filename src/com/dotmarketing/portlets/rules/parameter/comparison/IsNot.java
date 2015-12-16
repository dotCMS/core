package com.dotmarketing.portlets.rules.parameter.comparison;

/**
 * @author Geoff M. Granum
 */
public class IsNot extends Comparison<Comparable> {

    public IsNot() {
        super("isNot");
    }

    @Override
    public boolean perform(Comparable left, Comparable right) {
        return !java.util.Objects.equals(left, right);
    }
}
 
