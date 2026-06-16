package com.dotcms.rest.api.v1.page;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/**
 * Top-level view returned by {@code GET /api/v1/page/_render-sources/{uri}}.
 * Contains references only — no rendered HTML, no file content, no container code.
 *
 * <p>{@code containers} is a JSON object keyed by the container reference:
 * a UUID for DB containers and a host-qualified path for FILE containers.
 * Each value carries the {@code source} discriminator and the list of content types
 * that are actually placed on the page under the resolved persona and variant.
 *
 * <p>{@code urlContentMap} is present only when the requested URI was resolved via a
 * URL-map pattern (e.g. {@code /blog/post/my-post} matched a Blog content type pattern).
 * In that case {@code page.identifier} is the detail page, {@code page.uri} is the
 * originally requested URI (host-qualified), and {@code urlContentMap} identifies the
 * specific mapped contentlet.
 */
public class PageRenderSourcesView {

    @Schema(description = "Lightweight page reference. "
            + "When the request URI was a URL-mapped path, identifier is the detail page identifier "
            + "and uri is the originally requested URI (host-qualified).")
    private final PageSourceRefView page;

    @Schema(description = "Theme folder and its VTL files")
    private final ThemeSourceView theme;

    @Schema(description = "Containers referenced by the page template, keyed by container identifier "
            + "(UUID for DB containers, host-qualified path for FILE containers). "
            + "Only content types placed on the page appear inside each entry.")
    private final Map<String, ContainerSourceView> containers;

    @Schema(description = "Widget contentlets placed on the page")
    private final List<WidgetSourceView> widgets;

    @Schema(description = "Present only when the requested URI resolved via a URL-map pattern. "
            + "Identifies the specific mapped contentlet that owns the URL. "
            + "Absent for regular (literal) HTML page requests.")
    private final UrlContentMapView urlContentMap;

    public PageRenderSourcesView(
            final PageSourceRefView page,
            final ThemeSourceView theme,
            final Map<String, ContainerSourceView> containers,
            final List<WidgetSourceView> widgets) {
        this(page, theme, containers, widgets, null);
    }

    public PageRenderSourcesView(
            final PageSourceRefView page,
            final ThemeSourceView theme,
            final Map<String, ContainerSourceView> containers,
            final List<WidgetSourceView> widgets,
            final UrlContentMapView urlContentMap) {
        this.page           = page;
        this.theme          = theme;
        this.containers     = containers;
        this.widgets        = widgets;
        this.urlContentMap  = urlContentMap;
    }

    public PageSourceRefView getPage() {
        return page;
    }

    public ThemeSourceView getTheme() {
        return theme;
    }

    public Map<String, ContainerSourceView> getContainers() {
        return containers;
    }

    public List<WidgetSourceView> getWidgets() {
        return widgets;
    }

    @JsonInclude(Include.NON_NULL)
    public UrlContentMapView getUrlContentMap() {
        return urlContentMap;
    }
}
