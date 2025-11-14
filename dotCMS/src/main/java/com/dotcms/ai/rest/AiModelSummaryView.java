package com.dotcms.ai.rest;

import java.util.Objects;

/**
 * Summary to return the model availables in the advance config
 * @author jsanca
 */
public class AiModelSummaryView {

    private final String vendor;
    private final String name;
    private final String vendorModelPath;

    private AiModelSummaryView(final Builder builder) {
        this.vendor = builder.vendor;
        this.name = builder.name;
        this.vendorModelPath = builder.vendorModelPath;
    }

    public String getVendor() {
        return vendor;
    }

    public String getName() {
        return name;
    }

    public String getVendorModelPath() {
        return vendorModelPath;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String vendor;
        private String name;
        private String vendorModelPath;

        public Builder vendor(final String vendor) {
            this.vendor = vendor;
            return this;
        }

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder vendorModelPath(final String vendorModelPath) {
            this.vendorModelPath = vendorModelPath;
            return this;
        }

        public AiModelSummaryView build() {
            Objects.requireNonNull(vendor, "vendor is required");
            Objects.requireNonNull(name, "name is required");
            Objects.requireNonNull(vendorModelPath, "vendorModelPath is required");
            return new AiModelSummaryView(this);
        }
    }

    @Override
    public String toString() {
        return vendor + " - " + name + " (" + vendorModelPath + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(vendor, name, vendorModelPath);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AiModelSummaryView other = (AiModelSummaryView) obj;
        return Objects.equals(vendor, other.vendor)
                && Objects.equals(name, other.name)
                && Objects.equals(vendorModelPath, other.vendorModelPath);
    }
}
