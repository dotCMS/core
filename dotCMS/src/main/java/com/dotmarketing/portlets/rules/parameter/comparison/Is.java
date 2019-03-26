package com.dotmarketing.portlets.rules.parameter.comparison;

/** @author Geoff M. Granum */
public class Is extends Comparison<Object> {

  public Is() {
    super("is");
  }

  @Override
  public boolean perform(Object expect, Object right) {
    return java.util.Objects.equals(expect, right);
  }
}
