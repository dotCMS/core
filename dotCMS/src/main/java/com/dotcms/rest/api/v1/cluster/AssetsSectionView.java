package com.dotcms.rest.api.v1.cluster;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The shared assets folder as one server sees it.
 *
 * @author hassandotcms
 */
@Schema(description = "Shared assets folder as a server sees it")
public record AssetsSectionView(
        @Schema(description = "Assets folder path", nullable = true)
        String path,
        @Schema(description = "Whether the server can read the folder", nullable = true)
        Boolean canRead,
        @Schema(description = "Whether the server can write to the folder", nullable = true)
        Boolean canWrite) {
}
