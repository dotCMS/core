package com.dotcms.auth.dotAuth.rest;

import java.util.List;

/**
 * Envelope returned by {@code GET /v1/dotauth/sites}. Combines the SYSTEM_HOST
 * status (the global default) with the per-site status list used by the
 * dotAuth portlet's list view.
 */
public class DotAuthSitesView {

    private final SystemView system;
    private final List<SiteRowView> sites;

    public DotAuthSitesView(final SystemView system, final List<SiteRowView> sites) {
        this.system = system;
        this.sites = sites;
    }

    public SystemView getSystem() {
        return system;
    }

    public List<SiteRowView> getSites() {
        return sites;
    }

    /** Status of the SYSTEM_HOST (global default) row. */
    public static class SystemView {

        private final boolean configured;
        private final DotAuthProtocol protocol;
        private final boolean headlessConfigured;

        public SystemView(final boolean configured,
                          final DotAuthProtocol protocol,
                          final boolean headlessConfigured) {
            this.configured = configured;
            this.protocol = protocol;
            this.headlessConfigured = headlessConfigured;
        }

        public boolean isConfigured() {
            return configured;
        }

        public DotAuthProtocol getProtocol() {
            return protocol;
        }

        public boolean isHeadlessConfigured() {
            return headlessConfigured;
        }
    }

    /** One row per non-system site. */
    public static class SiteRowView {

        private final String hostId;
        private final String hostName;
        private final DotAuthSiteStatus status;
        private final DotAuthProtocol protocol;

        public SiteRowView(final String hostId,
                           final String hostName,
                           final DotAuthSiteStatus status,
                           final DotAuthProtocol protocol) {
            this.hostId = hostId;
            this.hostName = hostName;
            this.status = status;
            this.protocol = protocol;
        }

        public String getHostId() {
            return hostId;
        }

        public String getHostName() {
            return hostName;
        }

        public DotAuthSiteStatus getStatus() {
            return status;
        }

        public DotAuthProtocol getProtocol() {
            return protocol;
        }
    }
}
