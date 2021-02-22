package com.dotcms.util.diff;

import java.util.Map;

/**
 * Abstracts the logic to apply a diff between collections
 *
 * Usually the logic should be:
 *
 *   if it is on currentObjects but not in newObjects then delete
 *   if it is on newObjects but not in currentObjects then add
 *   if it is on both, but it is diff  then update
 *
 * @param <K>
 * @param <T>
 */
public interface DiffCommand <K,T> {

    /**
     *
     * @param currentObjects
     * @param newObjects
     * @return
     */
    DiffResult<K, T> applyDiff (final Map<K, T> currentObjects, final Map<K, T> newObjects);
}
