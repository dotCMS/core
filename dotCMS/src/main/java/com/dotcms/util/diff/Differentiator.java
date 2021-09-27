package com.dotcms.util.diff;

import java.util.Collection;

/**
 * Similar idea of a {@link java.util.Comparator} to determine if two objects are diff.
 * @param <T>
 */
public interface Differentiator<T> {

    /**
     * Determines if t1 is diff to t2. True if both are diff, otherwise false
     * @param t1
     * @param t2
     * @return
     */
    Collection<DiffItem> diff (T t1, T t2);
}
