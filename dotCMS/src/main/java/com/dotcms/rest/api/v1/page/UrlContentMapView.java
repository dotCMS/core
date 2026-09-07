package com.dotcms.rest.api.v1.page;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Present in the response only when the requested URI resolved via a URL-map pattern
 * (i.e. the URI matched a content-type URL map, not a literal HTML page asset).
 *
 * <p>The detail page (template, containers, theme) belongs to the content type's
 * configured detail page; {@code urlContentMap} identifies the specific mapped
 * contentlet that "owns" the URL so callers can retrieve its fields.
 *
 * <p>{@code contentletId} is the stable identifier across versions and languages.
 * {@code contentletInode} is the version-specific inode resolved under the requested
 * {@code language_id} and variant — use it with
 * {@code GET /api/v1/content/inode/{contentletInode}} to fetch the exact version.
 */
@JsonInclude(Include.NON_NULL)
public class UrlContentMapView {

    @Schema(description = "Velocity variable name of the URL-mapped Content Type "
            + "(e.g. 'Blog')")
    private final String contentTypeVar;

    @Schema(description = "Title of the URL-mapped contentlet")
    private final String title;

    @Schema(description = "Stable identifier of the URL-mapped contentlet "
            + "(same across all language/variant versions). "
            + "Use GET /api/v1/content/id/{contentletId} to retrieve it.")
    private final String contentletId;

    @Schema(description = "Inode of the exact version of the URL-mapped contentlet resolved "
            + "for this request (language- and variant-specific). "
            + "Use GET /api/v1/content/inode/{contentletInode} to retrieve the precise version.")
    private final String contentletInode;

    public UrlContentMapView(final String contentTypeVar, final String title,
            final String contentletId, final String contentletInode) {
        this.contentTypeVar  = contentTypeVar;
        this.title           = title;
        this.contentletId    = contentletId;
        this.contentletInode = contentletInode;
    }

    public String getContentTypeVar() {
        return contentTypeVar;
    }

    public String getTitle() {
        return title;
    }

    public String getContentletId() {
        return contentletId;
    }

    public String getContentletInode() {
        return contentletInode;
    }
}
