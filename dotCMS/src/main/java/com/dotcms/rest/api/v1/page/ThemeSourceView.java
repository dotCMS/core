package com.dotcms.rest.api.v1.page;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Theme metadata — folder reference plus every file that composes the theme.
 * No file content is included; each file carries its extension so consumers can
 * filter by type (vtl, css, scss, sass, js, ...) without the API whitelisting them.
 */
public class ThemeSourceView {

    @Schema(description = "Theme folder identifier")
    private final String id;

    @Schema(description = "Theme folder name")
    private final String name;

    @Schema(description = "Host-qualified path to the theme folder, e.g. //demo.dotcms.com/application/themes/travel/")
    private final String folderPath;

    @Schema(description = "Every file found anywhere under the theme folder (searched recursively), "
            + "regardless of type. Each entry carries its extension so consumers can filter.")
    private final List<FileRefView> files;

    public ThemeSourceView(final String id, final String name, final String folderPath,
            final List<FileRefView> files) {
        this.id         = id;
        this.name       = name;
        this.folderPath = folderPath;
        this.files      = files;
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

    public List<FileRefView> getFiles() {
        return files;
    }
}
