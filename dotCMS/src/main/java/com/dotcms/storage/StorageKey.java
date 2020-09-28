package com.dotcms.storage;

/**
 * Encapsulates a storage key, it has the key (for instance the path)
 * the group (for instance the bucket, folder, space, etc)
 * Storage {@link StorageType}
 * @author jsanca
 */
public class StorageKey {

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
