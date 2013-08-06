package com.dotmarketing.portlets.folders.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/** @author Hibernate CodeGenerator */
public class Folder extends Inode implements Serializable, Permissionable, Treeable {

	private static final long serialVersionUID = 1L;

    /** nullable persistent field */
    private String name;


    /** nullable persistent field */
    private int sortOrder;

    /** nullable persistent field */
    private boolean showOnMenu;

    /** nullable persistent field */
    private String hostId = "";


    private String title;
    /** default constructor */

    private String filesMasks;

    private String defaultFileType;

    private Date modDate;


	public Folder() {
    	this.setType("folder");
    }

	/**
	 * Returns the inode.
	 * @return String
	 */
	public String getInode() {
		return inode;
	}

	/**
	 * Returns the name.
	 * @return String
	 */
	public String getName() {
		return name;
	}



	/**
	 * Returns the sortOrder.
	 * @return int
	 */
	public int getSortOrder() {
		return sortOrder;
	}

	/**
	 * Sets the inode.
	 * @param inode The inode to set
	 */
	public void setInode(String inode) {
		this.inode = inode;
	}

	/**
	 * Sets the name.
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}


	/**
	 * Sets the sortOrder.
	 * @param sortOrder The sortOrder to set
	 */
	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	/**
	 * Returns the showOnMenu.
	 * @return boolean
	 */
	public boolean isShowOnMenu() {
		return showOnMenu;
	}

	/**
	 * Sets the showOnMenu.
	 * @param showOnMenu The showOnMenu to set
	 */
	public void setShowOnMenu(boolean showOnMenu) {
		this.showOnMenu = showOnMenu;
	}

	/**
	 * Returns the title.
	 * @return String
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the title.
	 * @param title The title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
		return ToStringBuilder.reflectionToString(this);
    }

	/**
	 * @return Returns the hostId.
	 */
	public String getHostId() {
		return hostId;
	}
	/**
	 * @param hostId The hostId to set.
	 */
	public void setHostId(String hostId) {
		if(!InodeUtils.isSet(hostId)){
			try {
				hostId = APILocator.getIdentifierAPI().find(this.identifier).getHostId();
			} catch (Exception e) {
				Logger.error(Folder.class, "Unable to get Identifier", e);
				throw new DotRuntimeException(e.getMessage(), e);
			}
		}
		this.hostId = hostId;
	}
	public void setIdentifier(String identifier) {
	   this.identifier = identifier;
	   setHostId(this.hostId);
	}

	public void copy (Folder template) {
		this.setHostId(template.getHostId());
		this.setName(template.getName());
		this.setShowOnMenu(template.isShowOnMenu());
		this.setSortOrder(template.getSortOrder());
		this.setTitle(template.getTitle());
		this.setDefaultFileType(template.getDefaultFileType());
	}

	public String getFilesMasks() {
		return filesMasks;
	}

	public void setFilesMasks(String filesMasks) {
		this.filesMasks = filesMasks;
	}


	public String getDefaultFileType() {
		return defaultFileType;
	}

	public void setDefaultFileType(String defaultFileType) {
		this.defaultFileType = defaultFileType;
	}

    public Date getModDate() {
		return modDate;
	}

	public void setModDate(Date modDate) {
		this.modDate = modDate;
	}

	public Map<String, Object> getMap() throws DotStateException, DotDataException, DotSecurityException {
        Map<String, Object> retMap = super.getMap();
        retMap.put("filesMasks", this.filesMasks);
        retMap.put("name", this.name);
        retMap.put("title", this.title);
        retMap.put("hostId", this.hostId);
        retMap.put("showOnMenu", this.showOnMenu);
        retMap.put("sortOrder", this.sortOrder);
        retMap.put("defaultFileType", this.defaultFileType);
        return retMap;
    }

    //Methods from permissionable and parent permissionable

	@Override
	public List<PermissionSummary> acceptedPermissions() {
		List<PermissionSummary> accepted = new ArrayList<PermissionSummary>();
		accepted.add(new PermissionSummary("view", "view-permission-description", PermissionAPI.PERMISSION_READ));
		accepted.add(new PermissionSummary("edit", "edit-permission-description", PermissionAPI.PERMISSION_WRITE));
		accepted.add(new PermissionSummary("publish", "publish-permission-description", PermissionAPI.PERMISSION_PUBLISH));
		accepted.add(new PermissionSummary("edit-permissions", "edit-permissions-permission-description", PermissionAPI.PERMISSION_EDIT_PERMISSIONS));
		return accepted;
	}

	@Override
	public boolean isParentPermissionable() {
		return true;
	}

	@Override
	public Permissionable getParentPermissionable() throws DotDataException {

		User systemUser = APILocator.getUserAPI().getSystemUser();

		FolderAPI folderAPI = APILocator.getFolderAPI();
		Folder parentFolder;
		try {
			parentFolder = folderAPI.findParentFolder(this, APILocator.getUserAPI().getSystemUser(), false);
		} catch (Exception e) {
			Logger.error(Folder.class, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}
		if(parentFolder != null)
			return parentFolder;

		try {
			return APILocator.getHostAPI().findParentHost(this, systemUser, false);
		} catch (DotSecurityException e) {
			Logger.error(Folder.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	public String getPath() {

		Identifier id = null;

		try {
			id = APILocator.getIdentifierAPI().find(this.getIdentifier());
		} catch (DotDataException e) {
			Logger.error(Folder.class, e.getMessage(), e);
		} catch (Exception e) {
			Logger.debug(this, " This is usually not a problem as it is usually just the identifier not being found" +  e.getMessage(), e);
		}

		return id!=null?id.getPath():null;
	}

}
