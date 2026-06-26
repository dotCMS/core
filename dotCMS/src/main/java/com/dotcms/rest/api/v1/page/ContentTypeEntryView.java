package com.dotcms.rest.api.v1.page;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Unified per-type entry inside a container's {@code contentTypes} list.
 * <ul>
 *   <li>DB containers: only {@code contentTypeVar} is present.</li>
 *   <li>FILE containers: {@code contentTypeVar}, {@code path}, and {@code identifier} are present
 *       (the VTL file that renders this type). To retrieve VTL content use
 *       {@code GET /api/v2/assets}.</li>
 * </ul>
 * Only content types that are actually placed on the page (per the resolved persona and variant)
 * appear in this list — types that are allowed by the container but not placed are omitted.
 */
@JsonInclude(Include.NON_NULL)
public class ContentTypeEntryView {

    @Schema(description = "Velocity variable name of the Content Type")
    private final String contentTypeVar;

    @Schema(description = "Host-qualified path to the VTL file (FILE containers only)")
    private final String path;

    @Schema(description = "File asset identifier of the VTL file (FILE containers only)")
    private final String identifier;

    /** DB-container constructor — path and identifier will be omitted from JSON. */
    public ContentTypeEntryView(final String contentTypeVar) {
        this.contentTypeVar = contentTypeVar;
        this.path           = null;
        this.identifier     = null;
    }

    /** FILE-container constructor. */
    public ContentTypeEntryView(final String contentTypeVar,
            final String path, final String identifier) {
        this.contentTypeVar = contentTypeVar;
        this.path           = path;
        this.identifier     = identifier;
    }

    public String getContentTypeVar() {
        return contentTypeVar;
    }

    public String getPath() {
        return path;
    }

    public String getIdentifier() {
        return identifier;
    }
}
