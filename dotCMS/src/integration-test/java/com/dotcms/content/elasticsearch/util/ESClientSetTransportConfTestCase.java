package com.dotcms.content.elasticsearch.util;

import com.dotcms.cluster.bean.Server;

public class ESClientSetTransportConfTestCase {
    private final String transportHostFromExtSettings;
    private final String transportTCPPortFromExtSettings;
    private final Server currentServer;
    private final String expectedTransportHost;
    private final String expectedTransportTCPPort;

    private ESClientSetTransportConfTestCase(String transportHostFromExtSettings, String transportTCPPortFromExtSettings, Server currentServer, String expectedTransportHost, String expectedTransportTCPPort) {
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

        Builder withTransportHostFromExtSettings(final String transportHostFromExtSettings) {
            this.transportHostFromExtSettings = transportHostFromExtSettings;
            return this;
        }

        Builder withTransportTCPPortFromExtSettings(final String transportTCPPortFromExtSettings) {
            this.transportTCPPortFromExtSettings = transportTCPPortFromExtSettings;
            return this;
        }

        Builder withCurrentServer(final Server currentServer) {
            this.currentServer = currentServer;
            return this;
        }

        Builder withExpectedTransportHost(final String expectedTransportHost) {
            this.expectedTransportHost = expectedTransportHost;
            return this;
        }

        Builder withExpectedTransportTCPPort(final String expectedTransportTCPPort) {
            this.expectedTransportTCPPort = expectedTransportTCPPort;
            return this;
        }

        ESClientSetTransportConfTestCase build() {
            return new ESClientSetTransportConfTestCase(transportHostFromExtSettings, transportTCPPortFromExtSettings,
                    currentServer, expectedTransportHost, expectedTransportTCPPort);
        }
    }

    String getTransportHostFromExtSettings() {
        return transportHostFromExtSettings;
    }

    public String getTransportTCPPortFromExtSettings() {
        return transportTCPPortFromExtSettings;
    }

    Server getCurrentServer() {
        return currentServer;
    }

    String getExpectedTransportHost() {
        return expectedTransportHost;
    }

    String getExpectedTransportTCPPort() {
        return expectedTransportTCPPort;
    }
}
