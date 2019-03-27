package com.dotmarketing.business;

public class PermissionableObjectDWR {
	
	private String id;
	private String type;
	private Boolean isParentPermissionable;
	private Boolean doesUserHavePermissionsToEdit;
	private Boolean isFolder;
	private Boolean isHost;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Boolean getIsParentPermissionable() {
		return isParentPermissionable;
	}
	public void setIsParentPermissionable(Boolean isParentPermissionable) {
		this.isParentPermissionable = isParentPermissionable;
	}
	public Boolean getDoesUserHavePermissionsToEdit() {
		return doesUserHavePermissionsToEdit;
	}
	public void setDoesUserHavePermissionsToEdit(Boolean doesUserHavePermissionsToEdit) {
		this.doesUserHavePermissionsToEdit = doesUserHavePermissionsToEdit;
	}
	public Boolean getIsFolder() {
		return isFolder;
	}
	public void setIsFolder(Boolean isFolder) {
		this.isFolder = isFolder;
	}
	public Boolean getIsHost() {
		return isHost;
	}
	public void setIsHost(Boolean isHost) {
		this.isHost = isHost;
	}

}
