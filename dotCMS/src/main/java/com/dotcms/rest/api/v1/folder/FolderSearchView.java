package com.dotcms.rest.api.v1.folder;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * REST view of a folder returned by the unified search endpoint
 * ({@code GET /api/v1/folder/search}).
 *
 * <p>Unlike the legacy {@link FolderSearchResultView} (used by {@code /byPath}),
 * this view exposes the folder {@code name} and its parent {@code path} separately,
 * matching the structure documented in the OpenAPI spec.
 *
 * <p>{@code defaultBaseType} carries the folder's Content Drive upload-mode preference (a
 * {@code BaseContentType} name such as {@code DOTASSET}/{@code FILEASSET}, or {@code null} for
 * no preference), so the Content Drive sidebar can read it without a second request.
 *
 * <p>{@code title}, {@code sortOrder}, {@code filesMasks}, {@code defaultFileType} and
 * {@code showOnMenu} are the fields the Content Drive "Edit folder" dialog pre-populates from.
 * They are read straight off the already-loaded {@code Folder} and are <b>always</b> present.
 *
 * <p>{@code permissions} is opt-in via the {@code includePermissions} query param, and its two
 * absent-value states are <b>not</b> equivalent:
 * <ul>
 *   <li>{@code null} — permissions were <b>not requested</b>. Nothing can be inferred about the
 *       user's grants.</li>
 *   <li>{@code []} — permissions were requested and the user has <b>no</b> grants on the folder.</li>
 * </ul>
 * Consumers must map {@code null} to an empty collection before testing membership rather than
 * treating the two states alike.
 */
public record FolderSearchView(
        @Schema(description = "Folder identifier") String id,
        @Schema(description = "Folder inode") String inode,
        @Schema(description = "Folder name (last path segment)") String name,
        @Schema(description = "Parent path of the folder, e.g. '/application/' for '/application/blog/'") String path,
        @Schema(description = "True when the requesting user has CAN_ADD_CHILDREN on this folder") boolean addChildrenAllowed,
        @Schema(description = "True when the folder has at least one child folder readable by the requesting user") boolean hasChildren,
        @Schema(description = "Content Drive upload-mode preference as a BaseContentType name (e.g. DOTASSET, FILEASSET); null when the folder has no preference") String defaultBaseType,
        @Schema(description = "Folder title") String title,
        @Schema(description = "Folder sort order used when ordering menu items") int sortOrder,
        @Schema(description = "Comma-separated file-name masks allowed in this folder, e.g. '*.jpg,*.png'") String filesMasks,
        @Schema(description = "Velocity variable name of the Content Type used by default for new files in this folder") String defaultFileType,
        @Schema(description = "True when the folder is shown on navigation menus") boolean showOnMenu,
        @ArraySchema(
                arraySchema = @Schema(description = "Permission types the requesting user holds on this folder. "
                        + "Only populated when 'includePermissions=true'; null means the permissions were not "
                        + "requested, while an empty array means they were requested and the user holds none."),
                schema = @Schema(allowableValues = {"READ", "EDIT", "PUBLISH", "EDIT_PERMISSIONS", "CAN_ADD_CHILDREN"}))
        List<String> permissions
) {

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link FolderSearchView}. The record has thirteen components, several of them
     * adjacent {@code String}s and {@code boolean}s — positional construction is easy to get
     * silently wrong, so callers name what they set.
     */
    public static final class Builder {

        private String id;
        private String inode;
        private String name;
        private String path;
        private boolean addChildrenAllowed;
        private boolean hasChildren;
        private String defaultBaseType;
        private String title;
        private int sortOrder;
        private String filesMasks;
        private String defaultFileType;
        private boolean showOnMenu;
        private List<String> permissions;

        private Builder() {}

        public Builder id(final String id) { this.id = id; return this; }
        public Builder inode(final String inode) { this.inode = inode; return this; }
        public Builder name(final String name) { this.name = name; return this; }
        public Builder path(final String path) { this.path = path; return this; }
        public Builder addChildrenAllowed(final boolean addChildrenAllowed) { this.addChildrenAllowed = addChildrenAllowed; return this; }
        public Builder hasChildren(final boolean hasChildren) { this.hasChildren = hasChildren; return this; }
        public Builder defaultBaseType(final String defaultBaseType) { this.defaultBaseType = defaultBaseType; return this; }
        public Builder title(final String title) { this.title = title; return this; }
        public Builder sortOrder(final int sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder filesMasks(final String filesMasks) { this.filesMasks = filesMasks; return this; }
        public Builder defaultFileType(final String defaultFileType) { this.defaultFileType = defaultFileType; return this; }
        public Builder showOnMenu(final boolean showOnMenu) { this.showOnMenu = showOnMenu; return this; }
        public Builder permissions(final List<String> permissions) { this.permissions = permissions; return this; }

        public FolderSearchView build() {
            return new FolderSearchView(id, inode, name, path, addChildrenAllowed, hasChildren,
                    defaultBaseType, title, sortOrder, filesMasks, defaultFileType, showOnMenu,
                    permissions);
        }
    }
}
