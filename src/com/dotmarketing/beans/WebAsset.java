package com.dotmarketing.beans;

import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 *
 * @author  maria
 */
public abstract class WebAsset extends Inode implements Permissionable, Versionable,Treeable{
	
	private static final long serialVersionUID = 1L;

	/** nullable persistent field */
    private String title = "";
    
 	/** nullable persistent field */
    private String friendlyName = "";
    
    /** persistent field */
    private java.util.Date modDate;

    /** nullable persistent field */
    private String modUser = "";
    
    /** nullable persistent field */
    private int sortOrder;

    /** nullable persistent field */
    private boolean showOnMenu;
    
    

    public String getVersionId() {
    	return getIdentifier();
    }
    
	/**
	 * Returns the deleted.
	 * @return boolean
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 * @throws DotStateException 
	 */
	public boolean isDeleted() throws DotStateException, DotDataException, DotSecurityException {
		return APILocator.getVersionableAPI().isDeleted(this);
	}
	
	public boolean isArchived() throws DotStateException, DotDataException, DotSecurityException {
        return isDeleted();
	}

	/**
	 * Returns the live.
	 * @return boolean
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 * @throws DotStateException 
	 */
	public boolean isLive() throws DotStateException, DotDataException, DotSecurityException {
	    return APILocator.getVersionableAPI().isLive(this);
	}

	/**
	 * Returns the locked.
	 * @return boolean
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 * @throws DotStateException 
	 */
	public boolean isLocked() throws DotStateException, DotDataException, DotSecurityException {
        return APILocator.getVersionableAPI().isLocked(this);
    }

	/**
	 * Returns the modDate.
	 * @return java.util.Date
	 */
	public java.util.Date getModDate() {
		return modDate;
	}

	/**
	 * Returns the modUser.
	 * @return String
	 */
	public String getModUser() {
		return modUser;
	}

	/**
	 * Returns the working.
	 * @return boolean
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 * @throws DotStateException 
	 */
	public boolean isWorking() throws DotStateException, DotDataException, DotSecurityException {
        return APILocator.getVersionableAPI().isWorking(this);
	}

	/**
	 * Sets the modDate.
	 * @param modDate The modDate to set
	 */
	public void setModDate(java.util.Date modDate) {
		this.modDate = modDate;
	}

	/**
	 * Sets the modUser.
	 * @param modUser The modUser to set
	 */
	public void setModUser(String modUser) {
		this.modUser = modUser;
	}

	public void copy(WebAsset currentWebAsset) {		
		this.modUser = currentWebAsset.getModUser();
		this.friendlyName = currentWebAsset.getFriendlyName();
		this.showOnMenu = currentWebAsset.isShowOnMenu();
		this.sortOrder = currentWebAsset.getSortOrder();
		this.modDate = new java.util.Date();
		this.title = currentWebAsset.getTitle();
        Logger.debug(WebAsset.class, "Calling WebAsset Copy Method"+this.modDate);
	}
	
	public abstract String getURI(Folder folder) throws DotStateException, DotDataException;

	/**
	 * Returns the showOnMenu.
	 * @return boolean
	 */
	public boolean isShowOnMenu() {
		return showOnMenu;
	}
	/**
	 * Returns the sort_order.
	 * @return int
	 */
	public int getSortOrder() {
		return sortOrder;
	}

	/**
	 * Returns the title.
	 * @return String
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the showOnMenu.
	 * @param showOnMenu The showOnMenu to set
	 */
	public void setShowOnMenu(boolean showOnMenu) {
		this.showOnMenu = showOnMenu;
	}

	/**
	 * Sets the sort_order.
	 * @param sort_order The sort_order to set
	 */
	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	/**
	 * Sets the title.
	 * @param title The title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Returns the friendlyName.
	 * @return String
	 */
	public String getFriendlyName() {
		return friendlyName;
	}
	

	/**
	 * Sets the friendlyName.
	 * @param friendlyName The friendlyName to set
	 */
	public void setFriendlyName(String friendlyName) {
		if (!UtilMethods.isSet(friendlyName))
			this.friendlyName = title;
		else
			this.friendlyName = friendlyName;
	}
	
	//The owner for webassets belong to the identifier
	@Override
	public String getOwner() {
		/*Identifier id;
		try {*/
			if(UtilMethods.isSet(owner)){
				return owner;
			}
			/*else if(InodeUtils.isSet(getIdentifier())) {
				id = APILocator.getIdentifierAPI().find(getIdentifier());
				if(InodeUtils.isSet(id.getInode()))
					return id.getOwner();
			}

		} catch (Exception e) {
			Logger.error(this, "Unable to retrieve the identifier.", e);
		}*/
		return "";
	}


	
	/**
	 * Returns a map representation of the asset
	 * @return the map
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 * @throws DotStateException 
	 */
	public Map<String, Object> getMap () throws DotStateException, DotDataException, DotSecurityException {
		
		Map<String, Object> map = super.getMap();
		
		map.put("title", this.title);
		map.put("friendlyName", this.friendlyName);
		map.put("live", isLive());
		map.put("working", isWorking());
		map.put("deleted", isDeleted());
		map.put("locked", isLocked());
		map.put("modDate", this.modDate);
		map.put("modUser", this.modUser);
		User modUser = null;
		try {
			modUser = APILocator.getUserAPI().loadUserById(this.getModUser(),APILocator.getUserAPI().getSystemUser(),false);
		} catch (NoSuchUserException e) {
			Logger.debug(this, e.getMessage());
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
		}
		if (UtilMethods.isSet(modUser) && UtilMethods.isSet(modUser.getUserId()) && !modUser.isNew())
			map.put("modUserName", modUser.getFullName());
		else
			map.put("modUserName", "unknown");
		map.put("sortOrder", this.sortOrder);
		map.put("showOnMenu", this.showOnMenu);
		
		return map;
	}
	public String getPermissionId() {
		return getIdentifier();
	}

	public String getVersionType() {
		return getType();
	}
}
    