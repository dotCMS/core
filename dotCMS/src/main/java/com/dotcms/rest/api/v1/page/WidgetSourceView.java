package com.dotcms.rest.api.v1.page;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Reference to a Widget contentlet placed on the page.
 *
 * <p>The {@code source} field indicates where the widget's Velocity code lives:
 * <ul>
 *   <li>{@code FILE} — the widget's VTL lives in a file asset; {@code path} and
 *       {@code identifier} are populated so callers can retrieve the source via
 *       {@code GET /api/v2/assets}.</li>
 *   <li>{@code CODE} — the widget's Velocity is inline (e.g. the {@code widgetCode} field
 *       on the content type and/or the contentlet's own fields). Callers should
 *       retrieve the content type definition via
 *       {@code GET /api/v1/contenttype/id/{contentTypeVar}} and the placed contentlet via
 *       {@code GET /api/v1/content/id/{contentletId}}.</li>
 * </ul>
 * {@code path} and {@code identifier} are omitted for {@code CODE} widgets.
 *
 * <p>{@code contentletId} is the stable identifier across all versions and languages.
 * {@code contentletInode} is version-specific — it identifies the exact
 * language/variant copy of the contentlet resolved for this request
 * (based on the requested {@code language_id} and {@code variantName}).
 * Use it to fetch the precise placed version via
 * {@code GET /api/v1/content/inode/{contentletInode}}.
 */
@JsonInclude(Include.NON_NULL)
public class WidgetSourceView {

    @Schema(description = "Velocity variable name of the Widget Content Type")
    private final String contentTypeVar;

    @Schema(description = "Title of the placed Widget contentlet")
    private final String title;

    @Schema(description = "Stable identifier of the placed Widget contentlet (same across all "
            + "language and variant versions). "
            + "Use GET /api/v1/content/id/{contentletId} to retrieve the contentlet.")
    private final String contentletId;

    @Schema(description = "Inode of the exact version of the placed Widget contentlet resolved "
            + "for this request (language- and variant-specific). "
            + "Use GET /api/v1/content/inode/{contentletInode} to retrieve the precise placed version.")
    private final String contentletInode;

    @Schema(description = "Source of the widget Velocity code. "
            + "FILE: VTL lives in a file asset (see path/identifier). "
            + "CODE: Velocity is inline in the content type's widgetCode field and/or "
            + "contentlet fields — retrieve via GET /api/v1/contenttype/id/{contentTypeVar} "
            + "and GET /api/v1/content/id/{contentletId}.",
            allowableValues = {"FILE", "CODE"})
    private final String source;

    @Schema(description = "Host-qualified path to the widget VTL file (FILE source only)")
    private final String path;

    @Schema(description = "File asset identifier of the widget VTL file (FILE source only)")
    private final String identifier;

    /**
     * FILE-backed widget: all fields populated.
     */
    public WidgetSourceView(final String contentTypeVar, final String title,
            final String contentletId, final String contentletInode,
            final String path, final String identifier) {
        this.contentTypeVar  = contentTypeVar;
        this.title           = title;
        this.contentletId    = contentletId;
        this.contentletInode = contentletInode;
        this.source          = "FILE";
        this.path            = path;
        this.identifier      = identifier;
    }

    /**
     * CODE widget: no file reference; {@code path} and {@code identifier} are null and will be
     * omitted from JSON serialization.
     */
    public WidgetSourceView(final String contentTypeVar, final String title,
            final String contentletId, final String contentletInode) {
        this.contentTypeVar  = contentTypeVar;
        this.title           = title;
        this.contentletId    = contentletId;
        this.contentletInode = contentletInode;
        this.source          = "CODE";
        this.path            = null;
        this.identifier      = null;
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

    public String getSource() {
        return source;
    }

    public String getPath() {
        return path;
    }

    public String getIdentifier() {
        return identifier;
    }
}
