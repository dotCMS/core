package com.dotcms.rest.api.v1.page;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Reference to a file asset — path, identifier and extension only, no content.
 * The {@code extension} lets consumers filter by type (e.g. vtl, css, scss, sass, js)
 * without the API having to whitelist which extensions a theme may contain.
 */
public class FileRefView {

    @Schema(description = "Host-qualified path to the file, e.g. //demo.dotcms.com/application/themes/travel/header.vtl")
    private final String path;

    @Schema(description = "File asset identifier")
    private final String identifier;

    @Schema(description = "Lowercased file extension without the leading dot, e.g. vtl, css, scss, js")
    private final String extension;

    public FileRefView(final String path, final String identifier, final String extension) {
        this.path       = path;
        this.identifier = identifier;
        this.extension  = extension;
    }

    public String getPath() {
        return path;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getExtension() {
        return extension;
    }
}
