package com.dotcms.auth.dotAuth.rest;

import java.util.Map;

/**
 * Full configuration returned by {@code GET /v1/dotauth/sites/{hostId}}. Hidden
 * secrets ({@code clientSecret}) are masked as {@code "****"}. The {@code
 * inherited} flag is set when the requested host has no row of its own but
 * SYSTEM_HOST does, in which case {@code values} holds the system defaults so
 * the portlet can pre-populate the form when admins break inheritance.
 */
public class DotAuthConfigView {

    private final String hostId;
    private final boolean configured;
    private final boolean inherited;
    private final Map<String, Object> values;

    public DotAuthConfigView(final String hostId,
                             final boolean configured,
                             final boolean inherited,
                             final Map<String, Object> values) {
        this.hostId = hostId;
        this.configured = configured;
        this.inherited = inherited;
        this.values = values;
    }

    public String getHostId() {
        return hostId;
    }

    public boolean isConfigured() {
        return configured;
    }

    public boolean isInherited() {
        return inherited;
    }

    public Map<String, Object> getValues() {
        return values;
    }
}
