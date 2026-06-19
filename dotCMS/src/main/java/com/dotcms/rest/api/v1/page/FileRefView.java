package com.dotcms.rest.api.v1.page;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Reference to a file asset (VTL, CSS, JS, etc.) — path and identifier only, no content.
 */
public class FileRefView {

    @Schema(description = "Host-qualified path to the file, e.g. //demo.dotcms.com/application/themes/travel/header.vtl")
    private final String path;

    @Schema(description = "File asset identifier")
    private final String identifier;

    public FileRefView(final String path, final String identifier) {
        this.path       = path;
        this.identifier = identifier;
    }

    public String getPath() {
        return path;
    }

    public String getIdentifier() {
        return identifier;
    }
}
