package com.dotmarketing.portlets.rules.parameter.comparison;

import com.dotmarketing.portlets.rules.conditionlet.Location;
import com.dotmarketing.portlets.rules.conditionlet.Location.UnitOfDistance;

public class WithinDistanceComparison extends Comparison<Location> {

    public WithinDistanceComparison() {
        super("withinDistance");
    }

    @Override
    public boolean perform(Location argA, Location argB, double distance, UnitOfDistance unitOfDistance) {
        return argB.within(argA, distance, unitOfDistance);
    }
}
