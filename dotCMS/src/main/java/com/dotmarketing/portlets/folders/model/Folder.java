package com.dotmarketing.portlets.folders.model;

import com.dotcms.api.tree.Parentable;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.manifest.ManifestItem;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.RelatedPermissionableGroup;
import com.dotmarketing.business.Ruleable;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.struts.FolderForm;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.builder.ToStringBuilder;


@JsonIgnoreProperties(ignoreUnknown = true)
public class Folder implements Serializable, Permissionable, Treeable, Ruleable,
		Parentable, ManifestItem {

	private static final long serialVersionUID = 1L;

	public static final String SYSTEM_FOLDER = "SYSTEM_FOLDER";

	public static final String SYSTEM_FOLDER_PATH = StringPool.FORWARD_SLASH;

	private String identifier;

    /** nullable persistent field */
    private String name;

    /** nullable persistent field */
    private int sortOrder;

    /** nullable persistent field */
    private boolean showOnMenu;

    /** nullable persistent field */
    private String hostId = "";

	public static final String FOLDER_TYPE = "folder";

	private String type;

    private String title;

    private String filesMasks;

    private String defaultFileType;

    private Date modDate;

	private String owner;

	private Date iDate;

	private String inode;

	private String path;

	@Override
	public String getOwner() {
		return owner;
	}

	@Override
	public void setOwner(String owner) {
		this.owner = owner;
	}

	public Date getIDate() {
		return iDate;
	}

	public void setIDate(Date iDate) {
		this.iDate = iDate;
	}

	public void setInode(String inode) {
		this.inode = inode;
	}

	@Override
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Returns the type.
	 *
	 * @return String
	 */
	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}


    public Host getHost() {
        return Try.of(()->APILocator.getHostAPI().find(hostId, APILocator.systemUser(), false)).getOrNull();
    }
    
    public boolean isSystemFolder() {
        return Try.of(()->FolderAPI.SYSTEM_FOLDER.equals(identifier)).getOrElse(false);
    }


    
    
    
	public Folder() {
    	modDate = new Date();
		type = FOLDER_TYPE;
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

	@Override
	public boolean isParent() {
		return true;
	}

	@Override
	public List<Treeable> getChildren(User user, boolean live, boolean working, boolean archived, boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException {
		return APILocator.getTreeableAPI().loadAssetsUnderFolder(this,user,live,working, archived, respectFrontEndPermissions);
	}


	/**
	 * Returns the sortOrder.
	 * @return int
	 */
	public int getSortOrder() {
		return sortOrder;
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
		if(!InodeUtils.isSet(hostId) && UtilMethods.isSet(this.identifier)){
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
	   this.inode = UtilMethods.isSet(this.inode) ? this.inode : identifier;
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
		final Map<String, Object> map = new HashMap<>();
		map.put("inode", this.identifier);
		map.put("type", FOLDER_TYPE);
		map.put("identifier", this.identifier);
		map.put("owner", this.owner);
		map.put("iDate", iDate);
		map.put("filesMasks", this.filesMasks);
		map.put("name", this.name);
		map.put("title", this.title);
		map.put("hostId", this.hostId);
		map.put("showOnMenu", this.showOnMenu);
		map.put("sortOrder", this.sortOrder);
		map.put("defaultFileType", this.defaultFileType);
		map.put("path", this.getPath());
		map.put("modDate", this.getModDate());
        return map;
    }

    //Methods from permissionable and parent permissionable

	@JsonIgnore
	@Override
	public List<PermissionSummary> acceptedPermissions() {
		List<PermissionSummary> accepted = new ArrayList<>();
		accepted.add(new PermissionSummary("view", "view-permission-description", PermissionAPI.PERMISSION_READ));
		accepted.add(new PermissionSummary("edit", "edit-permission-description", PermissionAPI.PERMISSION_WRITE));
		accepted.add(new PermissionSummary("publish", "publish-permission-description", PermissionAPI.PERMISSION_PUBLISH));
		accepted.add(new PermissionSummary("edit-permissions", "edit-permissions-permission-description", PermissionAPI.PERMISSION_EDIT_PERMISSIONS));
		return accepted;
	}
	@JsonIgnore
	@Override
	public String getPermissionId() {
		return getInode();
	}

	@JsonIgnore
	@Override
	public boolean isParentPermissionable() {
		return true;
	}

	@Override
	@JsonIgnore
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
	@JsonIgnore
	public List<RelatedPermissionableGroup> permissionDependencies(
			int requiredPermission) {
		return null;
	}

	public String getPermissionType() {
		return this.getClass().getCanonicalName();
	}

	public String getPath() {
		if (UtilMethods.isSet(this.path)){
			return this.path;
		}

		Identifier id = null;

		try {
			id = APILocator.getIdentifierAPI().find(this.getIdentifier());
		} catch (DotDataException e) {
			Logger.error(Folder.class, e.getMessage(), e);
		} catch (Exception e) {
			Logger.debug(this, " This is usually not a problem as it is usually just the identifier not being found" +  e.getMessage(), e);
		}

		if (null != id){
			this.path = id.getPath();
		}

		return this.path;
	}

	public void setPath(final String path) {
		this.path = path;
	}
	
	public boolean equals(Object o){
		if (o == null)
			return false;
		if (this == o) 
			return true;
		if(o instanceof Folder){
			if(!this.name.equals(((Folder) o).name))
				return false;
			if(!this.defaultFileType.equals(((Folder) o).defaultFileType))
				return false;
			if(this.sortOrder != ((Folder) o).sortOrder)
				return false;
			if(this.showOnMenu != ((Folder) o).showOnMenu)
				return false;
			if(!this.hostId.equals(((Folder) o).hostId))
				return false;
			if(!this.title.equals(((Folder) o).title))
				return false;
			if((this.filesMasks == null && ((Folder) o).filesMasks != null && ((Folder)o).filesMasks != StringPool.BLANK)
                    || (this.filesMasks != null && !this.filesMasks.equals(((Folder) o).filesMasks)))
				return false;				
		}else if(o instanceof FolderForm){
			if(!this.name.equals(((FolderForm) o).getName()))
				return false;
			if(!this.defaultFileType.equals(((FolderForm) o).getDefaultFileType()))
				return false;
			if(this.sortOrder != ((FolderForm) o).getSortOrder())
				return false;
			if(!this.hostId.equals(((FolderForm) o).getHostId()))
				return false;
			if(!this.title.equals(((FolderForm) o).getTitle()))
				return false;
			if((this.filesMasks == null && ((FolderForm) o).getFilesMasks() != null && ((FolderForm)o).getFilesMasks() != "")
	                    || (this.filesMasks != null && !this.filesMasks.equals(((FolderForm) o).getFilesMasks())))	
				return false;				
			if(!this.showOnMenu == ((FolderForm) o).isShowOnMenu())
				return false;
		}else 
			return false;
		return true;
	}

	public ManifestInfo getManifestInfo(){
		Folder parent = null;

		try {
			final Permissionable parentPermissionable = this.getParentPermissionable();

			if (Folder.class.isInstance(parentPermissionable)) {
				parent = (Folder) parentPermissionable;
			}
		} catch (DotDataException e) {

		}

		return new ManifestInfoBuilder()
			.objectType(PusheableAsset.FOLDER.getType())
			.id(this.getIdentifier())
			.inode(this.getInode())
			.title(this.getTitle())
			.site(this.getHost())
			.folder(parent)
			.build();
	}

}
