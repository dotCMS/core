package com.dotcms.contenttype.model.type;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.elasticsearch.common.Nullable;
import org.immutables.value.Value;

import com.dotcms.contenttype.model.BaseContentType;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.org.exolab.castor.xml.schema.Structure;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.RelatedPermissionableGroup;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public abstract class BaseContent implements Serializable, Permissionable {

	static final long serialVersionUID = 1L;

	abstract String name();

	@Nullable abstract String inode();

	@Nullable abstract String description();

	@Value.Default
	boolean defaultStructure() {
		return false;
	}

	@Nullable abstract String pagedetail();

	@Value.Default
	boolean fixed() {
		return false;
	}

	@Value.Default
	boolean system() {
		return false;
	}

	abstract String velocityVarName();

	@Nullable abstract String urlMapPattern();

	@Nullable abstract String publishDateVar();

	@Nullable abstract String expireDateVar();

	abstract String owner();

	@Value.Default
	Date modDate() {
		return new Date();
	}

	abstract BaseContentType baseContentType();

	@Value.Default
	String host() {
		return "SYSTEM_HOST";
	}

	@Value.Default
	String folder() {
		return "SYSTEM_FOLDER";
	}
	
	public Permissionable permissionable(){
		return new PermissionableWrapper(this);
	}
	
	
	@Override
	public String getPermissionId() {
		return inode();
	}

	@Override
	public String getOwner() {
		return owner();
	}
	@Override
	public void setOwner(String x) {
		throw new DotStateException("Cannot change the owner for an immutable value");
	}

	@Override
	public List<PermissionSummary> acceptedPermissions() {
		return ImmutableList.of(new PermissionSummary("view", "view-permission-description", PermissionAPI.PERMISSION_READ),
				new PermissionSummary("edit", "edit-permission-description", PermissionAPI.PERMISSION_WRITE), new PermissionSummary(
						"publish", "publish-permission-description", PermissionAPI.PERMISSION_PUBLISH), new PermissionSummary(
						"edit-permissions", "edit-permissions-permission-description", PermissionAPI.PERMISSION_EDIT_PERMISSIONS));

	}

	@Override
	public Permissionable getParentPermissionable() throws DotDataException {
		try {

			if (UtilMethods.isSet(folder()) && !folder().equals("SYSTEM_FOLDER")) {

				return APILocator.getFolderAPI().find(folder(), APILocator.getUserAPI().getSystemUser(), false);

			} else if (UtilMethods.isSet(host()) && !host().equals("SYSTEM_HOST")) {

				try {
					return APILocator.getHostAPI().find(host(), APILocator.getUserAPI().getSystemUser(), false);
				} catch (DotSecurityException e) {
					Logger.debug(getClass(), e.getMessage(), e);
				}
			}
			return APILocator.getHostAPI().findSystemHost();
		} catch (Exception e) {
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public boolean isParentPermissionable() {
		return true;
	}

	@Override
	public List<RelatedPermissionableGroup> permissionDependencies(int requiredPermission) {
		return null;
	}

	@Override
	public String getPermissionType() {
		return Structure.class.getCanonicalName();
	}
}