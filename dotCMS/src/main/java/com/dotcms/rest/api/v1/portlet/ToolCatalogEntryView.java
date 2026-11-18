package com.dotcms.rest.api.v1.portlet;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One row of the tools catalog: a tool that can be placed in a navigation section, as the Tools
 * portlet's Available Tools panel shows it.
 *
 * @param id       the portlet id, for example {@code roles} or {@code c_press-releases}
 * @param title    the title in the caller's language, falling back to the registered name and
 *                 then to the id; never the raw translation key
 * @param isCustom {@code true} only for a tool an admin created through New Tool; {@code false}
 *                 for every tool that ships with the product, Language Variables included
 * @author hassandotcms
 */
@Schema(description = "One tool that can be placed in a navigation section")
public record ToolCatalogEntryView(
        @Schema(description = "Portlet id, e.g. roles or c_press-releases", example = "c_press-releases")
        String id,
        @Schema(description = "Title in the caller's language; falls back to the registered name, then to the id",
                example = "Press Releases")
        String title,
        @Schema(description = "True only for tools created through New Tool: registered in the database and "
                + "not declared in the product's portlet id registry. False for every shipped tool, "
                + "Language Variables included")
        boolean isCustom) {
}
