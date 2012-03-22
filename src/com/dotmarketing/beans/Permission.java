package com.dotmarketing.beans;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.dotmarketing.business.PermissionAPI;

/** 
 * 	@author Hibernate CodeGenerator
 * @author David H Torres (2009) 
 */
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

	/** persistent field */
    private long id;

    /** persistent field */
    private String inode;

    /** persistent field */
    private String roleId;

    /** persistent field */
    private int permission;
    
    private boolean isBitPermission = false;
    
    private String type;

    /** full constructor */
    public Permission(String type, String inode, String roleid, int permission) {
    	this.type = type;
        this.inode = inode;
        this.roleId = roleid;
        this.permission = permission;
    }

    public Permission(String type, String inode, String roleid, int permission, boolean isBitPermission) {
    	this(type, inode, roleid, permission);
    	this.isBitPermission = isBitPermission;
    }
    

    public Permission(String inode, String roleid, int permission) {
        this(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, inode, roleid, permission);
    }

    public Permission(String inode, String roleid, int permission, boolean isBitPermission) {
        this(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, inode, roleid, permission, isBitPermission);
    }
    
    /** default constructor */
    public Permission() {
    	this.type = PermissionAPI.INDIVIDUAL_PERMISSION_TYPE;
    }

    public String getInode() {
   		return this.inode;
    }

    public void setInode(String inode) {
        this.inode = inode;
    }

    public String getRoleId() {
        return this.roleId;
    }

    public void setRoleId(String roleid) {
        this.roleId = roleid;
    }
    
    public void setRoleId(long roleid) {
        this.roleId = String.valueOf(roleid);
    }

    public int getPermission() {
        return this.permission;
    }

    public void setPermission(int permission) {
        this.permission = permission;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

	/**
	 * Returns the id.
	 * @return long
	 */
	public long getId() {
		return id;
	}

	/**
	 * Sets the id.
	 * @param id The id to set
	 */
	public void setId(long id) {
		this.id = id;
	}
    
    @Override
	public boolean equals(Object other) {

        if (!(other instanceof Permission)) {
            return false;
        }

        Permission castOther = ( Permission ) other;

        return ((this.getInode().equalsIgnoreCase(castOther.getInode()))
        		&& (this.getRoleId().equalsIgnoreCase(castOther.getRoleId()))
        		&& (this.getPermission() == castOther.getPermission())
        		&& (this.getType().equals(castOther.getType())
        		&& (this.isBitPermission() == castOther.isBitPermission())
    		)
        
        );
    }

    @Override
    public int hashCode() {
    	int hashCode = 0;
    	if(this.getInode()!=null && this.getRoleId() !=null)
    		hashCode =(int)(this.getInode().hashCode() + (this.getRoleId().hashCode()) + this.getPermission());
    	return hashCode;
    }

	public void setBitPermission(boolean isBitPermission) {
		this.isBitPermission = isBitPermission;
	}

	public boolean isBitPermission() {
		return isBitPermission;
	}
    
	/**
	 * 
	 * @param permissionType
	 * @return
	 */
	public boolean matchesPermission (int permission) {
		return (!this.isBitPermission() && this.getPermission() == permission) ||
			(this.isBitPermission() && (this.getPermission() & permission) > 0);
	}

	/**
	 * Sets the type of permission, used for permissions inheritance, a type individual means
	 * the permission applies to the asset itself, other type of permission means
	 * the permission will be inherited by children permissionables of the same type in the 
	 * chain of inheritance 
	 * @param type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Returns the type of permission
	 * @return
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * Checks if it is an individual permission, that only applies to the asset itself
	 * @return
	 */
	public boolean isIndividualPermission() {
		return type.equals(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE);
	}
	
	public Map<String, Object> getMap() {
		HashMap<String, Object> theMap = new HashMap<String, Object>();
		theMap.put("id", this.id);
		theMap.put("inode", this.inode);
		theMap.put("isBitPermission", this.isBitPermission);
		theMap.put("permission", this.permission);
		theMap.put("roleId", this.roleId);
		theMap.put("type", this.type);
		return theMap;
	}

}
