package com.dotmarketing.portlets.rules.conditionlet;


public class Location implements Comparable {

    private final double latitude;
    private final double longitude;

    private final static double EARTH_DIAMETER = 2.0 * 6378.14;
    private final static double RAD_CONVERT = Math.PI / 180.0;

    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Calculate the create circle distance between two Locations on the surface of the earth.
     * @return The distance between the two points, in meters.
     */
    public static double greatCircleDistance(Location from, Location to) {
        double deltaLat, deltaLon;
        double temp;

        double lat1 = from.latitude;
        double lon1 = from.longitude;
        double lat2 = to.latitude;
        double lon2 = to.longitude;

        // convert degrees to radians
        lat1 *= RAD_CONVERT;
        lat2 *= RAD_CONVERT;

        // find the deltas
        deltaLat = lat2 - lat1;
        deltaLon = (lon2 - lon1) * RAD_CONVERT;

        // Find the great circle distance
        temp = Math.pow(Math.sin(deltaLat / 2.0), 2.0) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLon / 2.0), 2.0);
        return EARTH_DIAMETER * Math.atan2(Math.sqrt(temp), Math.sqrt(1.0 - temp));
    }

    /**
     * Check if the provided point is within a target distance from this Location.
     * @param loc the target location.
     * @param radiusMeters The comparison distance in meters.
     * @return True if the target location lies within a circle described by the
     * radius <code>radiusMeters</code> with this Location as the center point
     */
    public boolean within(Location loc, double radiusMeters) {
        double actualDistance = Location.greatCircleDistance(this, loc) * 1000.0; //
        return actualDistance <= radiusMeters;
    }

    @Override
    public int compareTo(Object o) {
        if(this == o) { return 0; }
        if(o == null || getClass() != o.getClass()) { return -1; }
        Location location = (Location)o;
        int latDelta = Double.compare(location.latitude, latitude);
        int longDelta = Double.compare(location.longitude, longitude);

        // Weight the latitude high enough so that we have something resembling a logical sort order.
        // It would be nice to compare by distance to a real point (e.g. 0,0), but that could get expensive.
        // 1,1   => 4,1   => 5;
        // 1,0   => 4,0   => 4;
        // 1,-1  => 4,-1  => 3;
        // 0,1   => 0,1   => 1;
        // 0,0   => 0,0   => 0;
        // 0,-1  => 0,-1  => -1;
        // -1,1  => -4,1  => -3;
        // -1,0  => -4,0  => -4;
        // -1,-1 => -4,-1 => -5;
        int result = latDelta * 4 + longDelta;
        return result == 0 ? 0 : ( result > 0 ? 1 : -1);
    }
}
