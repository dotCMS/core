package com.dotmarketing.beans;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Structure;

/**
 * 
 * This is just a wrapper class over a contentlet, it just offers nice methods to access host content specific fields like the host name but 
 * it underneath is just a piece of content
 * 
 * @author David H Torres
 */
public class Host extends Contentlet implements Permissionable {

	/**
     * 
     */
	private static final long serialVersionUID = 1L;

	public Host() {
		map.put(SYSTEM_HOST_KEY, false);
		Structure st = StructureCache.getStructureByVelocityVarName("Host");
		this.map.put(STRUCTURE_INODE_KEY, st.getInode());
		setDefault(false);
		setSystemHost(false);
	}

	public Host(Contentlet c) {
		super();
		this.map = c.getMap();
	}

	public static final String HOST_NAME_KEY = "hostName";

	public static final String IS_DEFAULT_KEY = "isDefault";

	public static final String ALIASES_KEY = "aliases";

	public static final String SYSTEM_HOST_KEY = "isSystemHost";

	public static final String HOST_THUMB_KEY = "hostThumbnail";
	
	public static final String SYSTEM_HOST = "SYSTEM_HOST";
	
	public static final String TAG_STORAGE = "tagStorage";

	@Override
	public String getInode() {
		return super.getInode();
	}

	public String getVersionType() {
		return new String("host");
	}

	public String getAliases() {
		return (String) map.get(ALIASES_KEY);
	}

	public void setAliases(String aliases) {
		map.put(ALIASES_KEY, aliases);
	}

	public String getHostname() {
		return (String) map.get(HOST_NAME_KEY);
	}

	public void setHostname(String hostname) {
		map.put(HOST_NAME_KEY, hostname);
	}

	public String getHostThumbnail() {
		return (String) map.get(HOST_THUMB_KEY);
	}

	public void setHostThumbnail(String thumbnailInode) {
		map.put(HOST_THUMB_KEY, thumbnailInode);
	}

	public boolean isDefault() {
		return (Boolean) map.get(IS_DEFAULT_KEY);
	}

	public void setDefault(boolean isDefault) {
		map.put(IS_DEFAULT_KEY, isDefault);
	}

	public String getStructureInode() {
		Structure st = StructureCache.getStructureByVelocityVarName("Host");
		return (String) st.getInode();
	}

	public boolean isSystemHost() {
		return (Boolean) map.get(SYSTEM_HOST_KEY);
	}

	public void setSystemHost(boolean isSystemHost) {
		map.put(SYSTEM_HOST_KEY, isSystemHost);
	}

	public void setStructureInode(String structureInode) {
		// No structure inode can be set different then the host structure inode
		// set by the constructor
	}

	public Map<String, Object> getMap() {
		Map<String, Object> hostMap = super.getMap();
		// Legacy property referenced as 'hostname' while really is 'hostName'
		hostMap.put("hostname", hostMap.get("hostName"));
		hostMap.put("type", "host");
		
		return hostMap;
	}

	/**
	 * @author David H Torres
	 */
	@Override
	public List<PermissionSummary> acceptedPermissions() {
		List<PermissionSummary> accepted = new ArrayList<PermissionSummary>();
		accepted.add(new PermissionSummary("view", "view-permission-description", PermissionAPI.PERMISSION_READ));
		accepted.add(new PermissionSummary("add-children", "add-children-permission-description", PermissionAPI.PERMISSION_WRITE));
		accepted.add(new PermissionSummary("edit", "edit-permission-description", PermissionAPI.PERMISSION_WRITE));
		accepted.add(new PermissionSummary("publish", "publish-permission-description", PermissionAPI.PERMISSION_PUBLISH));
		accepted.add(new PermissionSummary("create-virtual-link", "create-virtual-link-permission-description", PermissionAPI.PERMISSION_CREATE_VIRTUAL_LINKS));
		accepted.add(new PermissionSummary("edit-permissions", "edit-permissions-permission-description", PermissionAPI.PERMISSION_EDIT_PERMISSIONS));
		return accepted;
	}

	@Override
	public Permissionable getParentPermissionable() throws DotDataException {
		if (this.isSystemHost())
			return null;
		try {
			return APILocator.getHostAPI().findSystemHost();
		} catch (DotDataException e) {
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}
	
	public String getTagStorage() {
		return (String) map.get(TAG_STORAGE);
	}

	public void setTagStorage(String tagStorageId) {
		map.put(TAG_STORAGE, tagStorageId);
	}

}
