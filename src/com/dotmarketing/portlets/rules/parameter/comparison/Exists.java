package com.dotmarketing.portlets.rules.parameter.comparison;

/**
 * @author Geoff M. Granum
 */
public class Exists extends Comparison<Object> {

    public Exists() {
        super("exists");
    }

    @Override
    public boolean perform(Object arg) {
        return arg != null;
    }
}
 
