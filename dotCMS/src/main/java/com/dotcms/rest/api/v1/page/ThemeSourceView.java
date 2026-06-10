package com.dotcms.rest.api.v1.page;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Theme metadata — folder reference + VTL file list.  No file content is included.
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

    @Schema(description = "VTL files found directly in the theme folder")
    private final List<VtlFileRefView> vtls;

    public ThemeSourceView(final String id, final String name, final String folderPath,
            final List<VtlFileRefView> vtls) {
        this.id         = id;
        this.name       = name;
        this.folderPath = folderPath;
        this.vtls       = vtls;
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

    public List<VtlFileRefView> getVtls() {
        return vtls;
    }
}
