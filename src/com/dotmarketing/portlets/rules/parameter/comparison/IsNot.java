package com.dotmarketing.portlets.rules.parameter.comparison;

/**
 * @author Geoff M. Granum
 */
public class IsNot extends Comparison<Object> {

    public IsNot() {
        super("isNot");
    }

    @Override
    public boolean perform(Object left, Object right) {
        return !java.util.Objects.equals(left, right);
    }
}
 
