package com.dotcms.rest.api.v1.portlet;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One row of the tools catalog: a tool that can be placed in a navigation section, as the Tools
 * portlet's Available Tools panel shows it.
 *
 * @param id       the portlet id, for example {@code roles} or {@code c_press-releases}
 * @param title    the qualified title in the caller's language, including the parent when set
 * @param isCustom {@code true} only for a tool an admin created through New Tool; {@code false}
 *                 for every tool that ships with the product, Language Variables included
 * @param parent   the raw navigation parent key, or empty for an ungrouped tool
 * @param label    the localized child label without a parent prefix
 * @author hassandotcms
 */
@Schema(description = "One tool that can be placed in a navigation section")
public record ToolCatalogEntryView(
        @Schema(description = "Portlet id, e.g. roles or c_press-releases", example = "c_press-releases")
        String id,
        @Schema(description = "Qualified title in the caller's language, prefixed by the parent when configured; "
                + "falls back to the registered name, then to the id",
                example = "Press Releases")
        String title,
        @Schema(description = "True only for tools created through New Tool: registered in the database and "
                + "not declared in the product's portlet id registry. False for every shipped tool, "
                + "Language Variables included")
        boolean isCustom,
        @Schema(description = "Raw navigation parent label or language key; empty for no parent",
                example = "Identity-Access")
        String parent,
        @Schema(description = "Localized child label without the parent prefix", example = "Users")
        String label) {
}
