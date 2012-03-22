package com.dotmarketing.business;

import java.io.Serializable;

/**
 * 
 * Bean used to store information about a permission for displaying it on a page
 * @author davidtorresv
 *
 */
@SuppressWarnings("serial")
public class PermissionSummary implements Serializable {

	private String label;
	private String description;
	private int permission;
	
	public PermissionSummary() {
	}
	
	
	public PermissionSummary(String label, String description, int permissionBit) {
		super();
		this.label = label;
		this.description = description;
		this.permission = permissionBit;
	}


	/**
	 * Returns the label i18n key to be used to present the permission name
	 * @return
	 */
	public String getLabel() {
		return label;
	}
	/**
	 * Set the label i18nable key 
	 * @param label
	 */
	public void setLabel(String label) {
		this.label = label;
	}
	/**
	 * Returns the description i18n key to be used to present the permission description
	 * @return
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * Sets the description i18n key
	 * @param description
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * Returns the permission bit (I.E 1 = View permission)
	 * @return
	 */
	public int getPermission() {
		return permission;
	}
	/**
	 * Sets the permission bit
	 * @param permission
	 */
	public void setPermission(int permissionBit) {
		this.permission = permissionBit;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof PermissionSummary)) {
			return false;
		}
		PermissionSummary other = (PermissionSummary) obj;
		
		return other.getLabel().equals(this.getLabel()) && other.getPermission() == this.getPermission();
		
 	}
	
}
