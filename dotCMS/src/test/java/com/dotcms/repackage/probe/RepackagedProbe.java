package com.dotcms.repackage.probe;

import java.util.HashMap;

/**
 * Stands in for one of the third-party libraries shaded into the {@code com.dotcms.repackage}
 * namespace, so a test can ask for a type that lives inside the branch the trusted list takes
 * back out of {@code com.dotcms}.
 */
public class RepackagedProbe {

    private RepackagedProbe() {
        //Fixture, never instantiated
    }

    /**
     * An anonymous type declared inside this package. Its name ends in {@code $1}, the shape
     * XStream's wildcard translation leaves unmatched, and it extends a map so that falling
     * through the wildcard lands it on XStream's own defaults, where the map hierarchy is
     * allowed unconditionally.
     *
     * @return the anonymous type
     */
    public static Class<?> anonymousType() {
        return new HashMap<String, String>() {
            private static final long serialVersionUID = 20261103L;
        }.getClass();
    }
}
