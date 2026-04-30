package com.dotcms.auth.dotAuth.rest;

import java.util.Map;

/**
 * Full configuration returned by {@code GET /v1/dotauth/sites/{hostId}}. Hidden
 * secrets (e.g. {@code clientSecret}) are masked as {@code "****"}. The {@code
 * inherited} flag is set when the requested host has no row of its own but
 * SYSTEM_HOST does, in which case {@code values} holds the system defaults so
 * the portlet can pre-populate the form when admins break inheritance.
 *
 * <p>The {@code protocol} discriminator indicates which authentication protocol
 * the stored values correspond to ({@link DotAuthProtocol#OAUTH} or
 * {@link DotAuthProtocol#SAML}) and determines the shape of {@code values}.
 */
public class DotAuthConfigView {

    private final String hostId;
    private final DotAuthProtocol protocol;
    private final boolean configured;
    private final boolean inherited;
    private final Map<String, Object> values;
    private final Map<String, Object> headlessValues;

    public DotAuthConfigView(final String hostId,
                             final DotAuthProtocol protocol,
                             final boolean configured,
                             final boolean inherited,
                             final Map<String, Object> values,
                             final Map<String, Object> headlessValues) {
        this.hostId = hostId;
        this.protocol = protocol;
        this.configured = configured;
        this.inherited = inherited;
        this.values = values;
        this.headlessValues = headlessValues;
    }

    public String getHostId() {
        return hostId;
    }

    public DotAuthProtocol getProtocol() {
        return protocol;
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

    public Map<String, Object> getHeadlessValues() {
        return headlessValues;
    }
}
