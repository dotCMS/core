package com.dotcms.storage;

/**
 * Encapsulates a storage key, it has the key (for instance the path)
 * the group (for instance the bucket, folder, space, etc)
 * Storage {@link StorageType}
 * @author jsanca
 */
public class StorageKey {

    private final String key;
    private final String group;
    private final String storage;

    private StorageKey(final Builder builder) {

        this.key = builder.key;
        this.group = builder.group;
        this.storage    = builder.storage;
    }

    public String getKey() {
        return key;
    }

    public String getGroup() {
        return group;
    }

    public String getStorage() {
        return storage;
    }

    public static final class Builder {

        private String key;
        private String group;
        private String storage;

        public Builder key(final String key) {

            this.key = key;
            return this;
        }

        public Builder group(final String group) {

            this.group = group;
            return this;
        }

        public Builder storage(final String storage) {

            this.storage = storage;
            return this;
        }

        public Builder storage(final StorageType type) {

            this.storage(type.name());
            return this;
        }

        public StorageKey build () {

            return new StorageKey(this);
        }

    }
}
