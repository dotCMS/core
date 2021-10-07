package com.dotcms.util.diff;

import java.util.Map;

/**
 * Abstracts the logic to apply a diff between collections
 * <p>
 * Usually the logic should be:
 * <p>
 * if it is on currentObjects but not in newObjects then delete
 * if it is on newObjects but not in currentObjects then add
 * if it is on both, but it is diff  then update
 *
 * @param <ResultKey>   Result key: usually the same of Key
 * @param <DiffResults> Diff Result: result of the diff, could be same of Value
 * @param <Key>         Key of the collection, usually a string
 * @param <Value>       value
 */
public interface DiffCommand<ResultKey, DiffResults, Key, Value> {

    /**
     * @param currentObjects
     * @param newObjects
     * @return
     */
    DiffResult<ResultKey, DiffResults> applyDiff(final Map<Key, Value> currentObjects, final Map<Key, Value> newObjects);
}
