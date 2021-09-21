package com.dotcms.util.diff;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Contains the result of a Diff between to collections.
 * Says which objects to add, to update and to delete.
 * The objects that keep the same, are not included.
 * @param <K>
 * @param <T>
 */
public class DiffResult <K, T> {

    private final Map<K, T> toAdd;
    private final Map<K, T> toUpdate;
    private final Map<K, T> toDelete;

    private DiffResult (final Builder<K,T> builder) {

        this.toAdd    = builder.toAdd;
        this.toUpdate = builder.toUpdate;
        this.toDelete = builder.toDelete;
    }

    public Map<K, T> getToAdd() {
        return toAdd;
    }

    public Map<K, T> getToUpdate() {
        return toUpdate;
    }

    public Map<K, T> getToDelete() {
        return toDelete;
    }

    public static final class Builder<K,T> {

        private  Map<K, T> toAdd    = new LinkedHashMap<>();
        private  Map<K, T> toUpdate = new LinkedHashMap<>();
        private  Map<K, T> toDelete = new LinkedHashMap<>();

        public Builder<K, T> putToAdd (final K key, T value) {

            this.toAdd.put(key, value);
            return this;
        }

        public Builder<K, T> putAllToAdd (final  Map<K, T> toAddMap) {

            this.toAdd.putAll(toAddMap);
            return this;
        }

        public Builder<K, T> putToUpdate (final K key, T value) {

            this.toUpdate.put(key, value);
            return this;
        }

        public Builder<K, T> putAllToUpdate (final  Map<K, T> toAddMap) {

            this.toUpdate.putAll(toAddMap);
            return this;
        }

        public Builder<K, T> putToDelete (final K key, T value) {

            this.toDelete.put(key, value);
            return this;
        }

        public Builder<K, T> putAllToDelete (final  Map<K, T> toAddMap) {

            this.toDelete.putAll(toAddMap);
            return this;
        }

        public DiffResult<K, T> build () {
            return new DiffResult<K, T> (this);
        }
    }
}
