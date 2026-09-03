package com.dotcms.security.apps;

import java.io.Serializable;

/**
 * Captures a partial failure that the {@code /api/v1/apps} listing should report without preventing
 * the apps it could resolve from being listed.
 *
 * <p>Originally this only carried per-file failures from {@link AppDescriptorHelper#loadAppDescriptors()}
 * reading app YAML files, so the REST layer hardcoded a single error code. It now also carries the
 * case where this node cannot read the App secrets store (issue #36724), which is a different
 * condition an administrator needs to tell apart from a malformed descriptor — hence
 * {@link #getErrorCode()}.
 */
public class AppDescriptorLoadError implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Default code, kept for the YAML descriptor failures this class was introduced for. */
    public static final String DESCRIPTOR_LOAD_ERROR_CODE = "app-descriptor-load-error";

    /** The App secrets store exists but this node cannot read it. */
    public static final String SECRETS_STORE_UNREADABLE_ERROR_CODE = "app-secrets-store-unreadable";

    private final String fileName;
    private final String message;
    private final String errorCode;

    public AppDescriptorLoadError(final String fileName, final String message) {
        this(fileName, message, DESCRIPTOR_LOAD_ERROR_CODE);
    }

    public AppDescriptorLoadError(final String fileName, final String message,
            final String errorCode) {
        this.fileName = fileName;
        this.message = message;
        this.errorCode = errorCode;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
