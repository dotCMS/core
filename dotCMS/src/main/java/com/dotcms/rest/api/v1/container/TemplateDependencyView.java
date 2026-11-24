package com.dotcms.rest.api.v1.container;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A Template that uses a Container, as listed by {@code GET /api/v1/containers/dependencies}.
 *
 * <p>Every value is returned as saved: the client decides how to render it.
 *
 * @param inode      the Template's inode
 * @param identifier the Template's identifier
 * @param title      the Template's title, as saved
 * @param siteName   the host name of the Site the Template lives in, or {@code null} when the
 *                   Site cannot be resolved for the requesting user
 */
@Schema(description = "A template that uses the container")
public record TemplateDependencyView(
        @Schema(description = "Template inode") String inode,
        @Schema(description = "Template identifier") String identifier,
        @Schema(description = "Template title, as saved") String title,
        @Schema(description = "Host name of the template's site; null when the site is not resolvable for the requesting user", nullable = true) String siteName
) {

}
