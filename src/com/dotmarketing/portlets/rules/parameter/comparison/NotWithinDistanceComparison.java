package com.dotmarketing.portlets.rules.parameter.comparison;

import com.dotmarketing.portlets.rules.conditionlet.Location;
import com.dotmarketing.portlets.rules.conditionlet.Location.UnitOfDistance;

public class NotWithinDistanceComparison extends Comparison<Location> {

    public NotWithinDistanceComparison() {
        super("notWithinDistance");
    }

    @Override
    public boolean perform(Location argA, Location argB, double distance, UnitOfDistance unitOfDistance) {
        return !argB.within(argA, distance, unitOfDistance);
    }
}
