package com.dotmarketing.business;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Bean used used to stored permissions dependencies 
 * @author davidtorresv
 *
 */
public class RelatedPermissionableGroup implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private boolean requiresAll = true;
	private List<Permissionable> relatedPermissionables = new ArrayList<Permissionable>();
	private int relatedRequiredPermission = 0;
	
	/**
	 * If return true it means that the relatedRequiredPermission is necessary on all the relatedPermissionables in the list
	 * @return
	 */	
	public boolean isRequiresAll() {
		return requiresAll;
	}
	/**
	 * Sets the requires all attribute
	 */
	public void setRequiresAll(boolean requiresAll) {
		this.requiresAll = requiresAll;
	}
	/**
	 * Returns the list of required permissionables associated with this group
	 * @return
	 */
	public List<Permissionable> getRelatedPermissionables() {
		return relatedPermissionables;
	}
	/**
	 * Sets the list of related permissionables 
	 * @param relatedPermissionables
	 */
	public void setRelatedPermissionables(
			List<Permissionable> relatedPermissionables) {
		this.relatedPermissionables = relatedPermissionables;
	}
	/**
	 * Retrieves the required permission that should be granted to the user on the list of relatedPermissionables
	 * @return
	 */
	public int getRelatedRequiredPermission() {
		return relatedRequiredPermission;
	}
	/**
	 * Sets the required permission
	 * @param relatedRequiredPermission
	 */
	public void setRelatedRequiredPermission(int relatedRequiredPermission) {
		this.relatedRequiredPermission = relatedRequiredPermission;
	}
	

}
