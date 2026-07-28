package com.dotcms.rest.api.v1.folder;

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
 */
public record FolderSearchView(
        String id,
        String inode,
        String name,
        String path,
        boolean addChildrenAllowed,
        boolean hasChildren,
        String defaultBaseType
) {}