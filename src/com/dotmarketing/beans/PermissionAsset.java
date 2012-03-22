package com.dotmarketing.beans;

import java.io.Serializable;
import java.util.ArrayList;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author  maria
 */
public class PermissionAsset implements Serializable, Comparable {

    private static final long serialVersionUID = 1L;

	/** identifier field */
    private java.util.List permissions;

    /** identifier field */
    private Inode asset;

    /** identifier field */
    private String pathToMe;

    /** identifier field */
    private String identifier;
    
    /** default constructor */
    public PermissionAsset() {
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

	/**
	 * Returns the asset.
	 * @return WebAsset
	 */
	public Inode getAsset() {
		return asset;
	}

	/**
	 * Returns the permissions.
	 * @return java.util.List
	 */
	public java.util.List getPermissions() {
		return permissions;
	}

	/**
	 * Sets the asset.
	 * @param asset The asset to set
	 */
	public void setAsset(Inode asset) {
		this.asset = asset;
	}

	/**
	 * Sets the permissions.
	 * @param permissions The permissions to set
	 */
	public void setPermissions(java.util.List permissions) {
		this.permissions = permissions;
	}

	public int compareTo(Object object){

		if(!(object instanceof PermissionAsset))return -1;
		
		PermissionAsset permAsset = (PermissionAsset) object;
		
		return (permAsset.getAsset()).compareTo(this.getAsset());
		
	  //return returnVal;
	}

	/**
	 * Returns the pathToMe.
	 * @return String
	 */
	public String getPathToMe() {
        if(pathToMe == null) return "orphan";
		return pathToMe;
	}

	/**
	 * Sets the pathToMe.
	 * @param pathToMe The pathToMe to set
	 */
	public void setPathToMe(String pathToMe) {
		this.pathToMe = pathToMe;
	}

	/**
	 * @return Returns the identifier.
	 */
	public String getIdentifier() {
		return identifier;
	}
	/**
	 * @param identifier The identifier to set.
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	public boolean hasReadPermission(){
		ArrayList<Long> permissions = new ArrayList(this.getPermissions());
		for (Long permission : permissions) {
			if(permission >= 1){
				return true;
			}
		}
		return false;
	}
	public  boolean hasWritePermission(){
		ArrayList<Long> permissions = new ArrayList(this.getPermissions());
		for (Long permission : permissions) {
			if(permission >= 2){
				return true;
			}
		}
		return false;
	}
	public boolean hasPublish(){
		ArrayList<Long> permissions = new ArrayList(this.getPermissions());
		for (Long permission : permissions) {
			if(permission >= 4){
				return true;
			}
		}
		return false;
	}
}