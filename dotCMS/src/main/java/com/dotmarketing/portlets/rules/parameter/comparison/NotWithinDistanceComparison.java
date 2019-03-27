package com.dotmarketing.portlets.rules.parameter.comparison;

import com.dotmarketing.portlets.rules.conditionlet.Location;

/**
 * All distances / lengths are in meters.
 */
public class NotWithinDistanceComparison extends Comparison<Location> {

    public NotWithinDistanceComparison() {
        super("notWithinDistance");
    }

    @Override
    public boolean perform(Location argA, Location argB, double distance) {
        return !argB.within(argA, distance);
    }
}
