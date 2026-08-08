package com.dotcms.rest.api.v1.page;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Reference to a file asset — path, identifier and extension only, no content.
 * The {@code extension} lets consumers filter by type (e.g. vtl, css, scss, sass, js)
 * without the API having to whitelist which extensions a theme may contain.
 *
 * @param path       Host-qualified path to the file.
 * @param identifier File asset identifier.
 * @param extension  Lowercased file extension without the leading dot.
 */
public record FileRefView(
        @Schema(description = "Host-qualified path to the file, e.g. //demo.dotcms.com/application/themes/travel/header.vtl")
        String path,

        @Schema(description = "File asset identifier")
        String identifier,

        @Schema(description = "Lowercased file extension without the leading dot, e.g. vtl, css, scss, js")
        String extension) {
}
