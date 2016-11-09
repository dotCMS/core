package com.dotmarketing.business;

import java.util.List;

import com.dotmarketing.exception.DotDataException;

public interface Permissionable {

	/**
	 * Sets the id to be used for checking permissions 
	 * 
	 * @return
	 */
	String getPermissionId();
	
	String getOwner();
	
	void setOwner(String owner);

	/**
	 * Returns the list of accepted permission of this permissionable
	 * (I.E. content accepts permissions to view, edit, publish, and edit permissions)
	 * @return
	 */
	List<PermissionSummary> acceptedPermissions ();
	
	/**
	 * Based on the given required permission bit, this method should return the list of dependencies that should
	 * be check as well on related permissionables before grant the requiredPermission
	 * 
	 * @param requiredPermission
	 * @return
	 */
	List<RelatedPermissionableGroup> permissionDependencies(int requiredPermission);
	
	/**
	 * Retrieves the parent permissionable in the chain of permissions inheritance 
	 * @return
	 * @throws DotDataException 
	 */
	Permissionable getParentPermissionable() throws DotDataException;
	
	/**
	 * Returns the permission type which this permissionable can inherit from
	 * @return
	 */
	String getPermissionType();
	
	/**
	 * Returns true if other asset could inherit permissions from it
	 * @return
	 */
	boolean isParentPermissionable();
		
}
