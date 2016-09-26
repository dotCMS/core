package com.dotmarketing.portlets.rules.parameter.comparison;

/**
 * @author Geoff M. Granum
 */
public class Exists extends Comparison<Object> {

    public Exists() {
        super("exists", 0);
    }

    @Override
    public boolean perform(Object arg) {
        return arg != null;
    }
}
 
