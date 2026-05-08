package com.dotcms.security.apps;

import java.io.Serializable;

/**
 * Captures a per-file failure that occurred while {@link AppDescriptorHelper#loadAppDescriptors()}
 * was reading app YAML files. Used by the REST layer to report partial failures in the
 * {@code /api/v1/apps} response without preventing the successfully loaded apps from being listed.
 */
public class AppDescriptorLoadError implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String fileName;
    private final String message;

    public AppDescriptorLoadError(final String fileName, final String message) {
        this.fileName = fileName;
        this.message = message;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMessage() {
        return message;
    }
}
