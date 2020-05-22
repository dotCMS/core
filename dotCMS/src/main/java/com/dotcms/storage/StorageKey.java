package com.dotcms.storage;

public class StorageKey {

    private final String path;     // todo: rename to key
    private final String bucket;  // rename to group
    private final String storage;

    private StorageKey(final Builder builder) {

        this.path       = builder.path;
        this.bucket     = builder.bucket;
        this.storage    = builder.storage;
    }

    public String getPath() {
        return path;
    }

    public String getBucket() {
        return bucket;
    }

    public String getStorage() {
        return storage;
    }

    public static final class Builder {

        private String path;
        private String bucket;
        private String storage;

        public Builder path(final String path) {

            this.path = path;
            return this;
        }

        public Builder bucket(final String bucket) {

            this.bucket = bucket;
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
