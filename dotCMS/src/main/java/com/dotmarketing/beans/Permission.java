package com.dotmarketing.beans;

import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.templates.model.FileAssetTemplate;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/** 
 * 	@author Hibernate CodeGenerator
 * @author David H Torres (2009) 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
        setType( type );
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

    @JsonSetter("roleId")
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

    @Override
    public String toString() {
        return "Permission{" +
                "id=" + id +
                ", inode='" + inode + '\'' +
                ", roleId='" + roleId + '\'' +
                ", permission=" + permission +
                ", isBitPermission=" + isBitPermission +
                ", type='" + type + '\'' +
                '}';
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
    public void setType ( String type ) {

        if ( type != null && (type.equals( HTMLPageAsset.class.getCanonicalName() )) ) {
            /*
            Required because hibernate on selects is mapping to specific classes and we are
            trying to apply the same type of permissions to all the HTML pages types we have.
             */
            type = IHTMLPage.class.getCanonicalName();
        }

		if ( type != null && (type.equals( FileAssetContainer.class.getCanonicalName())
				|| type.equals( FileAssetTemplate.class.getCanonicalName() )) ) {
            // file asset container/template are contentlets, so we have to map to contentlet
			type = Contentlet.class.getCanonicalName();
		}

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

	@JsonIgnore
	public Map<String, Object> getMap() {
		HashMap<String, Object> theMap = new HashMap<>();
		theMap.put("id", this.id);
		theMap.put("inode", this.inode);
		theMap.put("isBitPermission", this.isBitPermission);
		theMap.put("permission", this.permission);
		theMap.put("roleId", this.roleId);
		theMap.put("type", this.type);
		return theMap;
	}

}
