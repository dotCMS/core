package com.dotcms.rest.api.v1.page;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Theme metadata — folder reference plus the files that compose the theme (VTL, CSS and JS),
 * each split into its own list.  No file content is included.
 * To list all files under the theme folder use:
 * {@code GET /api/v1/folder/sitename/{site}/uri/{uri}}
 */
public class ThemeSourceView {

    @Schema(description = "Theme folder identifier")
    private final String id;

    @Schema(description = "Theme folder name")
    private final String name;

    @Schema(description = "Host-qualified path to the theme folder, e.g. //demo.dotcms.com/application/themes/travel/")
    private final String folderPath;

    @Schema(description = "VTL files found anywhere under the theme folder (searched recursively)")
    private final List<FileRefView> vtls;

    @Schema(description = "CSS files found anywhere under the theme folder (searched recursively)")
    private final List<FileRefView> css;

    @Schema(description = "JS files found anywhere under the theme folder (searched recursively)")
    private final List<FileRefView> js;

    public ThemeSourceView(final String id, final String name, final String folderPath,
            final List<FileRefView> vtls, final List<FileRefView> css,
            final List<FileRefView> js) {
        this.id         = id;
        this.name       = name;
        this.folderPath = folderPath;
        this.vtls       = vtls;
        this.css        = css;
        this.js         = js;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public List<FileRefView> getVtls() {
        return vtls;
    }

    public List<FileRefView> getCss() {
        return css;
    }

    public List<FileRefView> getJs() {
        return js;
    }
}
