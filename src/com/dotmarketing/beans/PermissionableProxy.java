package com.dotmarketing.beans;

import java.util.List;

import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.RelatedPermissionableGroup;
import com.dotmarketing.exception.DotDataException;

public  class PermissionableProxy implements Permissionable {
	
	private static final long serialVersionUID = 1L;
	
	public String Inode;
	
	public String Identifier;
	
	public Boolean permissionByIdentifier;
	
	public Boolean getPermissionByIdentifier() {
		return permissionByIdentifier;
	}

	public void setPermissionByIdentifier(Boolean permissionByIdentifier) {
		this.permissionByIdentifier = permissionByIdentifier;
	}

	public String getInode() {
		return Inode;
	}

	public String getIdentifier() {
		return Identifier;
	}

	public void setInode(String inode) {
		Inode = inode;
	}

	public void setIdentifier(String identifier) {
		Identifier = identifier;
	}
	
	public String getPermissionId() {
		String idReturn=null;
		if(permissionByIdentifier){
			idReturn=getIdentifier();
		}
		else{
			idReturn=getInode();
		}
		return idReturn;
	}

	public List<PermissionSummary> acceptedPermissions() {
		return null;
	}

	public String getOwner() {
		return null;
	}

	public Permissionable getParentPermissionable() throws DotDataException {
		return null;
	}

	public String getPermissionType() {
		return null;
	}

	public List<RelatedPermissionableGroup> permissionDependencies(
			int requiredPermission) {
		return null;
	}

	public void setOwner(String owner) {
	}

	public List<? extends Permissionable> getChildrenPermissionable() throws DotDataException {
		return null;
	}

	public boolean isParentPermissionable() {
		return false;
	}

}
