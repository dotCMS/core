package com.dotmarketing.portlets.rules.conditionlet;

import com.liferay.util.Distance;

public class Location implements Comparable {

    public enum UnitOfDistance {
        MILES,
        KILOMETERS
    }

    private final double latitude;
    private final double longitude;

    private final static double EARTH_DIAMETER = 2 * 6378.2;
    private final static double PI = 3.14159265;
    private final static double RAD_CONVERT = PI / 180;

    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double distance(Location loc) {
        double deltaLat, deltaLon;
        double temp;

        double lat1 = latitude;
        double lon1 = longitude;
        double lat2 = loc.latitude;
        double lon2 = loc.longitude;

        // convert degrees to radians
        lat1 *= RAD_CONVERT;
        lat2 *= RAD_CONVERT;

        // find the deltas
        deltaLat = lat2 - lat1;
        deltaLon = (lon2 - lon1) * RAD_CONVERT;

        // Find the great circle distance
        temp = Math.pow(Math.sin(deltaLat / 2), 2) + Math.cos(lat1)
                * Math.cos(lat2) * Math.pow(Math.sin(deltaLon / 2), 2);
        return EARTH_DIAMETER
                * Math.atan2(Math.sqrt(temp), Math.sqrt(1 - temp));
    }

    public boolean within(Location loc, double inputDistance, UnitOfDistance unitOfDistance) {
        // distance in KM
        double actualDistance = this.distance(loc);

        if(unitOfDistance == UnitOfDistance.MILES) {
            actualDistance = Distance.kmToMiles(actualDistance);
        }

        return actualDistance <= inputDistance;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
