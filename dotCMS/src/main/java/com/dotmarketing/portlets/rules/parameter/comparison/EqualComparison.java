package com.dotmarketing.portlets.rules.parameter.comparison;

/** @author Geoff M. Granum */
public class EqualComparison extends Comparison<Comparable> {

  public EqualComparison() {
    super("equal");
  }

  @Override
  public boolean perform(Comparable expect, Comparable argB) {
    return expect.compareTo(argB) == 0;
  }
}
