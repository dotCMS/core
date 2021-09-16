package com.dotcms.cache;

/**
 * To be implemented by objects that are planned to be cached for a specific amount of time,
 * specified by the {@link #getTtl()}
 */
public interface Expirable {
    default long getTtl() {
        return -1;
    }
}
