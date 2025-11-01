package com.dotcms.ai.config.parser;

import java.util.*;

/**
 * Encapsulates the catalog of ai vendor
 * @author jsanca
 */
public final class AiVendorCatalogData {

    private final Map<String, String> routing;

    private final Map<String, AiVendorNode> vendors;

    private AiVendorCatalogData(Builder builder) {

        final Map<String, AiVendorNode> tmp = new LinkedHashMap<>();
        for (final Map.Entry<String, AiVendorNode> e : builder.vendors.entrySet()) {
            tmp.put(e.getKey(), e.getValue());
        }
        this.vendors = Collections.unmodifiableMap(tmp);
        this.routing = Collections.unmodifiableMap(builder.routing);
    }

    public Map<String, AiVendorNode> getVendors() {
        return vendors;
    }

    public Map<String, String> getRouting() {
        return routing;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, AiVendorNode> vendors = new LinkedHashMap<>();
        private final Map<String, String> routing = new HashMap<>();

        public Builder putVendor(String name, AiVendorNode vendorNode) {
            Objects.requireNonNull(name, "vendor name");
            Objects.requireNonNull(vendorNode, "vendorNode");
            vendors.put(name, vendorNode);
            return this;
        }

        public Builder routing(final Map<String, String> routing) {
            this.routing.putAll(routing);
            return this;
        }

        public AiVendorCatalogData build() {
            return new AiVendorCatalogData(this);
        }
    }
}
