package com.dotcms.ai.v2.api.configurations;

import java.util.*;

public final class VendorCatalogData {

    private final Map<String, VendorNode> vendors;

    private VendorCatalogData(Builder b) {
        Map<String, VendorNode> tmp = new LinkedHashMap<>();
        for (Map.Entry<String, VendorNode> e : b.vendors.entrySet()) {
            tmp.put(e.getKey(), e.getValue());
        }
        this.vendors = Collections.unmodifiableMap(tmp);
    }

    public Map<String, VendorNode> getVendors() {
        return vendors;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, VendorNode> vendors = new LinkedHashMap<>();

        public Builder putVendor(String name, VendorNode vendorNode) {
            Objects.requireNonNull(name, "vendor name");
            Objects.requireNonNull(vendorNode, "vendorNode");
            vendors.put(name, vendorNode);
            return this;
        }

        public VendorCatalogData build() {
            return new VendorCatalogData(this);
        }
    }
}
