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
) {}
