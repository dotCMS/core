package com.dotcms.content.elasticsearch.util;

import java.nio.file.Path;

class ESClientTestCase {
    private final boolean community;
    private final Path overrideFilePath;
    private final Path defaultFilePath;
    private final String expectedTransportHost;
    private final String expectedZenUniCastHosts;
    private final String expectedESNodeData;
    private final String expectedESNodeMaster;
    private final String expectedTCPPort;

    private ESClientTestCase(final Builder builder) {
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

        Builder community(final boolean community) {
            this.community = community;
            return this;
        }

        Builder overrideFilePath(final Path overrideFilePath) {
            this.overrideFilePath = overrideFilePath;
            return this;
        }

        Builder defaultFilePath(final Path defaultFilePath) {
            this.defaultFilePath = defaultFilePath;
            return this;
        }

        Builder expectedTransportHost(final String expectedTransportHost) {
            this.expectedTransportHost = expectedTransportHost;
            return this;
        }

        Builder expectedZenUniCastHosts(final String expectedZenUniCastHosts) {
            this.expectedZenUniCastHosts = expectedZenUniCastHosts;
            return this;
        }

        Builder expectedESNodeData(final String expectedESNodeData) {
            this.expectedESNodeData = expectedESNodeData;
            return this;
        }

        Builder expectedESNodeMaster(final String expectedESNodeMaster) {
            this.expectedESNodeMaster = expectedESNodeMaster;
            return this;
        }

        Builder expectedTCPPort(final String expectedTCPPort) {
            this.expectedTCPPort = expectedTCPPort;
            return this;
        }

        ESClientTestCase build() {
            return new ESClientTestCase(this);
        }
    }


    boolean isCommunity() {
        return community;
    }

    Path getOverrideFilePath() {
        return overrideFilePath;
    }

    Path getDefaultFilePath() {
        return defaultFilePath;
    }

    String getExpectedTransportHost() {
        return expectedTransportHost;
    }

    String getExpectedZenUniCastHosts() {
        return expectedZenUniCastHosts;
    }

    String getExpectedESNodeData() {
        return expectedESNodeData;
    }

    String getExpectedESNodeMaster() {
        return expectedESNodeMaster;
    }

    String getExpectedTCPPort() {
        return expectedTCPPort;
    }
}
