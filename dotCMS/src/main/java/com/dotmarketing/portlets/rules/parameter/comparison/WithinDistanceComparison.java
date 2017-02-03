package com.dotmarketing.portlets.rules.parameter.comparison;

import com.dotmarketing.portlets.rules.conditionlet.Location;

/**
 * All distances / lengths are in meters.
 */
public class WithinDistanceComparison extends Comparison<Location> {

    public WithinDistanceComparison() {
        super("withinDistance");
    }

    @Override
    public boolean perform(Location argA, Location argB, double distance) {
        return argB.within(argA, distance);
    }
}
