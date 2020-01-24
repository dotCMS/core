package com.dotcms.content.elasticsearch.business;

import org.elasticsearch.common.unit.ByteSizeValue;

public class NodeStats {
    private final String name;
    private final String host;
    private int size;
    private String prettySize;
    private String transportAddress;
    private boolean master;
    private int docCount;

    private NodeStats(final Builder builder) {
        this.name = builder.name;
        this.host = builder.host;
        this.size = builder.size;
        this.prettySize = new ByteSizeValue(size).toString();
        this.transportAddress = builder.transportAddress;
        this.master = builder.master;
        this.docCount = builder.docCount;
    }

    public String getHost() {
        return host;
    }

    public int getSizeRaw() {
        return size;
    }

    public String getSize() {
        return prettySize;
    }

    public String getTransportAddress() {
        return transportAddress;
    }

    public boolean isMaster() {
        return master;
    }

    public int getDocCount() {
        return docCount;
    }

    public String getName() {
        return name;
    }

    public static class Builder {
        private String name;
        private String host;
        private int size;
        private String transportAddress;
        private boolean master;
        private int docCount;

        public Builder() {}

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder host(final String host) {
            this.host = host;
            return this;
        }

        public Builder size(final int size) {
            this.size = size;
            return this;
        }

        public Builder transportAddress(final String transportAddress) {
            this.transportAddress = transportAddress;
            return this;
        }

        public Builder master(final boolean master) {
            this.master = master;
            return this;
        }

        public Builder docCount(final int docCount) {
            this.docCount = docCount;
            return this;
        }

        public NodeStats build() {
            return new NodeStats(this);
        }
    }
}
