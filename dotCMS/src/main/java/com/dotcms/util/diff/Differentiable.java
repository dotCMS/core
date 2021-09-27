package com.dotcms.util.diff;

/**
 * This interface just compares to objects (itself and the parameter on "t") and return true if
 * are diff, false if are the same.
 *
 * Usually this could be handle by {@link Comparable} but if the class already implement Comparator
 * for another reason (such as sorting) you can implement Diff, this will be have precedent over the Comparators
 * @param <T>
 */
public interface Differentiable <T> {


    /**
     * Returns true if t parameter is diff of this object
     * @param t  T
     * @return boolean
     */
    boolean isDiff (T t);
}
