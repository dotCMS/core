package com.dotcms.rest.api.v1.system.permission;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * View object representing metadata about a permissionable asset.
 * Used by the permissions tab to determine UI rendering (e.g., which
 * permission checkboxes to show, whether to show folder/host-specific options).
 *
 * <p>Replaces the legacy {@code PermissionableObjectDWR} used by DWR.
 */
public class PermissionableObjectView {

    private final String id;
    private final String type;
    private final boolean isFolder;
    private final boolean isHost;
    private final boolean isContentType;
    private final boolean isParentPermissionable;
    private final boolean doesUserHavePermissionsToEdit;

    public PermissionableObjectView(final String id,
                                     final String type,
                                     final boolean isFolder,
                                     final boolean isHost,
                                     final boolean isContentType,
                                     final boolean isParentPermissionable,
                                     final boolean doesUserHavePermissionsToEdit) {
        this.id = id;
        this.type = type;
        this.isFolder = isFolder;
        this.isHost = isHost;
        this.isContentType = isContentType;
        this.isParentPermissionable = isParentPermissionable;
        this.doesUserHavePermissionsToEdit = doesUserHavePermissionsToEdit;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    @JsonProperty("isFolder")
    public boolean isFolder() {
        return isFolder;
    }

    @JsonProperty("isHost")
    public boolean isHost() {
        return isHost;
    }

    @JsonProperty("isContentType")
    public boolean isContentType() {
        return isContentType;
    }

    @JsonProperty("isParentPermissionable")
    public boolean isParentPermissionable() {
        return isParentPermissionable;
    }

    @JsonProperty("doesUserHavePermissionsToEdit")
    public boolean isDoesUserHavePermissionsToEdit() {
        return doesUserHavePermissionsToEdit;
    }
}
