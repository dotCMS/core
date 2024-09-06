package com.dotcms.storage;

import java.io.Serializable;
import java.util.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Encapsulates a storage key, it has the key (for instance the path)
 * the group (for instance the bucket, folder, space, etc)
 * Storage {@link StorageType}
 * @author jsanca
 */
public class StorageKey implements Serializable {

    //Key components
    private final String path;
    private final String group;
    private final StorageType storage;

    /**
     * Builder based constructor
     * @param builder
     */
    private StorageKey(final Builder builder) {

        this.path = builder.path;
        this.group = builder.group;
        this.storage    = builder.storage;
    }

    /**
     * read only path
     * @return
     */
    public String getPath() {
        return path;
    }

    /**
     * read only group
     * @return
     */
    public String getGroup() {
        return group;
    }

    /**
     * read-only storage
     * @return
     */
    public StorageType getStorage() {
        return storage;
    }


    @Override
    public String toString() {
        return String.format(
               "StorageKey{ storage=`%s`,\n group=`%s`, \n path=`%s` \n }",
               storage, group, path
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StorageKey that = (StorageKey) o;

        if (!Objects.equals(path, that.path)) return false;
        if (!Objects.equals(group, that.group)) return false;
        return Objects.equals(storage, that.storage);
    }

    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + (group != null ? group.hashCode() : 0);
        result = 31 * result + (storage != null ? storage.hashCode() : 0);
        return result;
    }

    /**
     * Convenience builder
     */
    public static final class Builder {

        private String path;
        private String group;
        private StorageType storage;

        /**
         * path setter
         * @param path
         * @return
         */
        public Builder path(final String path) {

            this.path = path;
            return this;
        }

        /**
         * group setter
         * @param group
         * @return
         */
        public Builder group(final String group) {

            this.group = group;
            return this;
        }

        /**
         * storage setter
         * @param storage
         * @return
         */
        public Builder storage(final String storage) {

            this.storage = StorageType.valueOf(storage);
            return this;
        }

        /**
         * storage setter
         * @param type
         * @return
         */
        public Builder storage(final StorageType type) {

            this.storage = type;
            return this;
        }

        /**
         * Build method
         * @return
         */
        public StorageKey build () {
            return new StorageKey(this);
        }

    }
}
