package com.dotmarketing.beans;

import com.dotcms.api.tree.Parentable;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.dotmarketing.business.*;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

import io.vavr.control.Try;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * This is just a wrapper class over a contentlet, it just offers nice methods to access host content specific fields like the host name but
 * it underneath is just a piece of content
 *
 * @author David H Torres
 */
public class Host extends Contentlet implements Permissionable,Treeable,Parentable {

	public static final String SYSTEM_HOST_SITENAME = "System Host";

	/**
     *
     */
	private static final long serialVersionUID = 1L;

	public Host() {
		map.put(SYSTEM_HOST_KEY, false);
		Structure st = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");
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

	public static final String SYSTEM_HOST_NAME = com.dotmarketing.util.StringUtils.camelCaseLower(Host.SYSTEM_HOST);

	public static final String TAG_STORAGE = "tagStorage";
	
    public static final String HOST_VELOCITY_VAR_NAME = "Host";
    
    public static final String EMBEDDED_DASHBOARD = "embeddedDashboard";

	@Override
	public String getInode() {
		return super.getInode();
	}

	@Override
	public String getName() {
		return getTitle();
	}

	@Override
	public boolean isParent() {
		return true;
	}

	@Override
	public List<Treeable> getChildren(User user, boolean live, boolean working, boolean archived, boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException {
		return APILocator.getTreeableAPI().loadAssetsUnderHost(this,user,live,working, archived, respectFrontEndPermissions);
	}

	@JsonIgnore
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

	public File getHostThumbnail() {
		return (File) map.get(HOST_THUMB_KEY);
	}

	public void setHostThumbnail(File thumbnailInode) {
		map.put(HOST_THUMB_KEY, thumbnailInode);
	}

	public boolean isDefault() {
		return (Boolean) map.getOrDefault(IS_DEFAULT_KEY, Boolean.FALSE);
	}

	public void setDefault(boolean isDefault) {
		map.put(IS_DEFAULT_KEY, isDefault);
	}

	public String getStructureInode() {
		Structure st = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");
		return (String) st.getInode();
	}



	public boolean isSystemHost() {
		Object isSystemHost = map.get(SYSTEM_HOST_KEY);
		if(isSystemHost!=null) {
			if (isSystemHost instanceof Boolean) {
				return (Boolean) isSystemHost;
			}
			return Integer.parseInt(isSystemHost.toString()) == 1 ? true
					: false;
		}
		return false;
	}

	public void setSystemHost(boolean isSystemHost) {
		map.put(SYSTEM_HOST_KEY, isSystemHost);
	}

	public void setStructureInode(String structureInode) {
		// No structure inode can be set different then the host structure inode
		// set by the constructor
	}

	@JsonIgnore
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
	@JsonIgnore
	public List<PermissionSummary> acceptedPermissions() {
		List<PermissionSummary> accepted = new ArrayList<>();
		accepted.add(new PermissionSummary("view", "view-permission-description", PermissionAPI.PERMISSION_READ));
		accepted.add(new PermissionSummary("add-children", "add-children-permission-description", PermissionAPI.PERMISSION_WRITE));
		accepted.add(new PermissionSummary("edit", "edit-permission-description", PermissionAPI.PERMISSION_WRITE));
		accepted.add(new PermissionSummary("publish", "publish-permission-description", PermissionAPI.PERMISSION_PUBLISH));
		accepted.add(new PermissionSummary("edit-permissions", "edit-permissions-permission-description", PermissionAPI.PERMISSION_EDIT_PERMISSIONS));
		return accepted;
	}

	@JsonIgnore
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
		Host host = Try.of(()->
				APILocator.getHostAPI().find(map.get(TAG_STORAGE).toString(), APILocator.systemUser(), false)).getOrNull();

		if(UtilMethods.isSet(()->host.getIdentifier())) {
			return host.getIdentifier();
		}

		return Host.SYSTEM_HOST;
	}

	public void setTagStorage(String tagStorageId) {
		map.put(TAG_STORAGE, tagStorageId);
	}


	@Override
	public String toString() {
		return this.getHostname();
	}
}
