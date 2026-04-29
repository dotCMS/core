package com.dotcms.rest.api.v1.page;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Schema-only view of a single container entry in the body of
 * {@code POST /api/v1/page/{pageId}/content}.
 *
 * <p>The actual request body is deserialized by
 * {@link PageContainerForm.ContainerDeserialize}, which reads a bare JSON array
 * with field names {@code identifier}, {@code uuid}, {@code personaTag},
 * {@code contentletsId}. This class exists so Swagger advertises that exact
 * shape; it is never instantiated at runtime.
 */
@Schema(description = "Single container slot assignment for a page.")
public class PageContainerEntryView {

    @Schema(
            required = true,
            description = "Container's path-based key, e.g. " +
                    "'//demo.dotcms.com/application/containers/default/'. " +
                    "Matches the keys in 'page.containers' returned by " +
                    "GET /api/v1/page/json/{uri} (or /render/{uri}). Not the database UUID.",
            example = "//demo.dotcms.com/application/containers/default/")
    private String identifier;

    @Schema(
            required = true,
            description = "Slot instance identifier. Matches the 'uuid' in " +
                    "page.layout.body.rows[].columns[].containers[]. The same container " +
                    "can appear multiple times on a page with different UUIDs.",
            example = "10")
    private String uuid;

    @Schema(
            required = true,
            description = "Ordered contentlet identifiers placed in this slot. " +
                    "An empty array clears the slot. This is a full replacement: " +
                    "omitted slots are removed from the page.")
    private List<String> contentletsId;

    @Schema(
            description = "Optional persona tag for personalized content variants.",
            nullable = true)
    private String personaTag;

    public String getIdentifier() {
        return identifier;
    }

    public String getUuid() {
        return uuid;
    }

    public List<String> getContentletsId() {
        return contentletsId;
    }

    public String getPersonaTag() {
        return personaTag;
    }
}
