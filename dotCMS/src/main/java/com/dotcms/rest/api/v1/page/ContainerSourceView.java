package com.dotcms.rest.api.v1.page;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Map value in {@code PageRenderSourcesView.containers}.  The map key is the container reference
 * (UUID for DB containers, host-qualified path for FILE containers), so neither field is repeated
 * here.
 * <ul>
 *   <li>DB: {@code source="DB"}.  Retrieve container code via
 *       {@code GET /api/v1/containers/working?containerId=<id>&includeContentType=true}.</li>
 *   <li>FILE: {@code source="FILE"}.  Retrieve VTL content via {@code GET /api/v2/assets}.
 *       Each {@link ContentTypeEntryView} carries its own {@code path} and {@code identifier}.</li>
 * </ul>
 * Only content types that have at least one contentlet placed on the page (per the resolved
 * persona and variant) appear in {@code contentTypes}.
 */
public class ContainerSourceView {

    @Schema(description = "Container source type: DB or FILE", allowableValues = {"DB", "FILE"})
    private final String source;

    @Schema(description = "Content types placed on the page for this container. "
            + "DB entries contain only contentTypeVar; FILE entries also include path and identifier.")
    private final List<ContentTypeEntryView> contentTypes;

    public ContainerSourceView(final String source,
            final List<ContentTypeEntryView> contentTypes) {
        this.source       = source;
        this.contentTypes = contentTypes;
    }

    public String getSource() {
        return source;
    }

    public List<ContentTypeEntryView> getContentTypes() {
        return contentTypes;
    }
}
