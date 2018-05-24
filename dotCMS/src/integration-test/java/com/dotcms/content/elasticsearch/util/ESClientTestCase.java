package com.dotcms.content.elasticsearch.util;

import java.nio.file.Path;

class ESClientTestCase {
    private boolean community;
    private Path overrideFilePath;
    private Path defaultFilePath;
    private String expectedTransportHost;
    private String expectedZenUniCastHosts;
    private String expectedESNodeData;
    private String expectedESNodeMaster;
    private String expectedTCPPort;

    private ESClientTestCase(Builder builder) {
        this.community = builder.community;
        this.overrideFilePath = builder.overrideFilePath;
        this.defaultFilePath = builder.defaultFilePath;
        this.expectedTransportHost = builder.expectedTransportHost;
        this.expectedZenUniCastHosts = builder.expectedZenUniCastHosts;
        this.expectedESNodeData = builder.expectedESNodeData;
        this.expectedESNodeMaster = builder.expectedESNodeMaster;
        this.expectedTCPPort = builder.expectedTCPPort;
    }

    public static class Builder {
        private boolean community;
        private Path overrideFilePath;
        private Path defaultFilePath;
        private String expectedTransportHost;
        private String expectedZenUniCastHosts;
        private String expectedESNodeData;
        private String expectedESNodeMaster;
        private String expectedTCPPort;

        public Builder community(boolean community) {
            this.community = community;
            return this;
        }

        public Builder overrideFilePath(Path overrideFilePath) {
            this.overrideFilePath = overrideFilePath;
            return this;
        }

        public Builder defaultFilePath(Path defaultFilePath) {
            this.defaultFilePath = defaultFilePath;
            return this;
        }

        public Builder expectedTransportHost(String expectedTransportHost) {
            this.expectedTransportHost = expectedTransportHost;
            return this;
        }

        public Builder expectedZenUniCastHosts(String expectedZenUniCastHosts) {
            this.expectedZenUniCastHosts = expectedZenUniCastHosts;
            return this;
        }

        public Builder expectedESNodeData(String expectedESNodeData) {
            this.expectedESNodeData = expectedESNodeData;
            return this;
        }

        public Builder expectedESNodeMaster(String expectedESNodeMaster) {
            this.expectedESNodeMaster = expectedESNodeMaster;
            return this;
        }

        public Builder expectedTCPPort(String expectedTCPPort) {
            this.expectedTCPPort = expectedTCPPort;
            return this;
        }

        public ESClientTestCase build() {
            return new ESClientTestCase(this);
        }
    }


    public boolean isCommunity() {
        return community;
    }

    public Path getOverrideFilePath() {
        return overrideFilePath;
    }

    public Path getDefaultFilePath() {
        return defaultFilePath;
    }

    public String getExpectedTransportHost() {
        return expectedTransportHost;
    }

    public String getExpectedZenUniCastHosts() {
        return expectedZenUniCastHosts;
    }

    public String getExpectedESNodeData() {
        return expectedESNodeData;
    }

    public String getExpectedESNodeMaster() {
        return expectedESNodeMaster;
    }

    public String getExpectedTCPPort() {
        return expectedTCPPort;
    }
}
