package com.dotcms.content.elasticsearch.util;

import com.dotcms.cluster.bean.Server;

public class ESClientSetTransportConfTestCase {
    private String transportHostFromExtSettings;
    private String transportTCPPortFromExtSettings;
    private Server currentServer;
    private String expectedTransportHost;
    private String expectedTransportTCPPort;

    public ESClientSetTransportConfTestCase(String transportHostFromExtSettings, String transportTCPPortFromExtSettings, Server currentServer, String expectedTransportHost, String expectedTransportTCPPort) {
        this.transportHostFromExtSettings = transportHostFromExtSettings;
        this.transportTCPPortFromExtSettings = transportTCPPortFromExtSettings;
        this.currentServer = currentServer;
        this.expectedTransportHost = expectedTransportHost;
        this.expectedTransportTCPPort = expectedTransportTCPPort;
    }

    public static class Builder {
        private String transportHostFromExtSettings;
        private String transportTCPPortFromExtSettings;
        private Server currentServer;
        private String expectedTransportHost;
        private String expectedTransportTCPPort;

        public Builder withTransportHostFromExtSettings(final String transportHostFromExtSettings) {
            this.transportHostFromExtSettings = transportHostFromExtSettings;
            return this;
        }

        public Builder withTransportTCPPortFromExtSettings(final String transportTCPPortFromExtSettings) {
            this.transportTCPPortFromExtSettings = transportTCPPortFromExtSettings;
            return this;
        }

        public Builder withCurrentServer(final Server currentServer) {
            this.currentServer = currentServer;
            return this;
        }

        public Builder withExpectedTransportHost(final String expectedTransportHost) {
            this.expectedTransportHost = expectedTransportHost;
            return this;
        }

        public Builder withExpectedTransportTCPPort(final String expectedTransportTCPPort) {
            this.expectedTransportTCPPort = expectedTransportTCPPort;
            return this;
        }

        public ESClientSetTransportConfTestCase build() {
            return new ESClientSetTransportConfTestCase(transportHostFromExtSettings, transportTCPPortFromExtSettings,
                    currentServer, expectedTransportHost, expectedTransportTCPPort);
        }
    }

    public String getTransportHostFromExtSettings() {
        return transportHostFromExtSettings;
    }

    public String getTransportTCPPortFromExtSettings() {
        return transportTCPPortFromExtSettings;
    }

    public Server getCurrentServer() {
        return currentServer;
    }

    public String getExpectedTransportHost() {
        return expectedTransportHost;
    }

    public String getExpectedTransportTCPPort() {
        return expectedTransportTCPPort;
    }
}
