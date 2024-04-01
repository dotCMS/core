package com.dotcms.rest.api.v1.osgi;

import java.io.Serializable;

/**
 * Encapsulates a bundle map
 */
public class BundleMap implements Serializable {

    private final long bundleId;
    private final String symbolicName;

    private final String location;

    private final String jarFile;

    private final int state;

    private final String version;

    private final String separator;

    final boolean isSystem;

    public long getBundleId() {
        return bundleId;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public String getLocation() {
        return location;
    }

    public String getJarFile() {
        return jarFile;
    }

    public int getState() {
        return state;
    }

    public String getVersion() {
        return version;
    }

    public String getSeparator() {
        return separator;
    }

    public boolean isSystem() {
        return isSystem;
    }

    private BundleMap(final Builder builder) {
        this.bundleId = builder.bundleId;
        this.symbolicName = builder.symbolicName;
        this.location = builder.location;
        this.jarFile = builder.jarFile;
        this.state = builder.state;
        this.version = builder.version;
        this.separator = builder.separator;
        this.isSystem = builder.isSystem;
    }

    public static class Builder {
        private long bundleId;
        private String symbolicName;
        private String location;
        private String jarFile;
        private int state;
        private String version;
        private String separator;
        private boolean isSystem;

        public Builder bundleId(final long bundleId) {
            this.bundleId = bundleId;
            return this;
        }

        public Builder symbolicName(final String symbolicName) {
            this.symbolicName = symbolicName;
            return this;
        }

        public Builder location(final String location) {
            this.location = location;
            return this;
        }

        public Builder jarFile(final String jarFile) {
            this.jarFile = jarFile;
            return this;
        }

        public Builder state(final int state) {
            this.state = state;
            return this;
        }

        public Builder version(final String version) {
            this.version = version;
            return this;
        }

        public Builder separator(final String separator) {
            this.separator = separator;
            return this;
        }

        public Builder isSystem(final boolean isSystem) {
            this.isSystem = isSystem;
            return this;
        }

        public BundleMap build() {
            return new BundleMap(this);
        }
    }
}
