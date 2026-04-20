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

        public SystemView(final boolean configured) {
            this.configured = configured;
        }

        public boolean isConfigured() {
            return configured;
        }
    }

    /** One row per non-system site. */
    public static class SiteRowView {

        private final String hostId;
        private final String hostName;
        private final DotAuthSiteStatus status;

        public SiteRowView(final String hostId, final String hostName, final DotAuthSiteStatus status) {
            this.hostId = hostId;
            this.hostName = hostName;
            this.status = status;
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
    }
}
