package com.dotcms.contenttype.model.type;

import java.util.List;

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

public class PermissionableWrapper implements Permissionable {

	static final long serialVersionUID = 1L;

	final BaseContent contentType;


	public PermissionableWrapper(BaseContent contentType) {
		super();
		this.contentType = contentType;
	}
	
	@Override
	public String getPermissionId() {
		return contentType.inode();
	}

	@Override
	public String getOwner() {
		return contentType.owner();
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

			if (UtilMethods.isSet(contentType.folder()) && !contentType.folder().equals("SYSTEM_FOLDER")) {

				return APILocator.getFolderAPI().find(contentType.folder(), APILocator.getUserAPI().getSystemUser(), false);

			} else if (UtilMethods.isSet(contentType.host()) && !contentType.host().equals("SYSTEM_HOST")) {

				try {
					return APILocator.getHostAPI().find(contentType.host(), APILocator.getUserAPI().getSystemUser(), false);
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
