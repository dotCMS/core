package com.dotcms.rest.api.v1.container;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * The Templates that use a Container, as returned by {@code GET /api/v1/containers/dependencies}.
 *
 * <p>The Containers portlet shows this list before deleting a Container. Every value is
 * returned as saved: the client decides how to render it. A drawn Template is listed when its
 * layout references the Container; an advanced Template only when the Container holds content.
 *
 * @param inode      the Container's inode
 * @param identifier the Container's identifier
 * @param title      the Container's title, as saved
 * @param templates  the Templates that use the Container; empty when none does, never null
 */
@Schema(description = "A container and the templates that use it")
public record ContainerDependenciesView(
        @Schema(description = "Container inode") String inode,
        @Schema(description = "Container identifier") String identifier,
        @Schema(description = "Container title, as saved") String title,
        @ArraySchema(schema = @Schema(implementation = TemplateDependencyView.class),
                arraySchema = @Schema(description = "Templates that use the container; empty when none does"))
        List<TemplateDependencyView> templates
) {

    /**
     * Keeps an unmodifiable copy of the Template list.
     *
     * @throws NullPointerException if {@code templates} or one of its elements is null
     */
    public ContainerDependenciesView {
        templates = List.copyOf(templates);
    }
}
