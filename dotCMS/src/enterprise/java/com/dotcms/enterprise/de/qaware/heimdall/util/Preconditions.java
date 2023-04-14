package com.dotcms.enterprise.de.qaware.heimdall.util;

/**
 * Preconditions.
 */
public final class Preconditions {
    /**
     * No instances allowed.
     */
    private Preconditions() {
    }

    /**
     * Ensures that the given reference is not null.
     *
     * @param reference     Reference.
     * @param referenceName Name of the reference.
     * @param <T>           Type of the reference.
     * @return The reference.
     * @throws IllegalArgumentException If the reference is null.
     */
    public static <T> T checkNotNull(T reference, String referenceName) {
        if (reference == null) {
            throw new IllegalArgumentException(referenceName + " must not be null");
        }

        return reference;
    }
}
