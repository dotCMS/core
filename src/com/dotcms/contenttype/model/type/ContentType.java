package com.dotcms.contenttype.model.type;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.elasticsearch.common.Nullable;
import org.immutables.value.Value;

import com.dotcms.contenttype.model.field.Field;
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


public abstract class ContentType implements Serializable, Permissionable {

	static final long serialVersionUID = 1L;

	public abstract String name();

	@Nullable
	public abstract String inode();

	@Nullable
	public abstract String description();

	final public String type() {
		return "structure";
	}

	@Value.Default
	public boolean defaultStructure() {
		return false;
	}

	@Nullable
	public abstract String pagedetail();

	@Value.Default
	public boolean fixed() {
		return false;
	}

	@Value.Default
	public Date iDate() {
		return new Date();
	}

	@Value.Default
	public boolean system() {
		return false;
	}
	
	
	@Value.Default
	public boolean versionable(){
		return true;
	}
	
	@Value.Default
	public boolean multilingualable(){
		return false;
	}
	
	
	public abstract String velocityVarName();

	@Nullable
	public abstract String urlMapPattern();

	@Nullable
	public abstract String publishDateVar();

	@Nullable
	public abstract String expireDateVar();
	
	@Nullable
	public abstract String owner();

	@Value.Default
	public Date modDate() {
		return new Date();
	}

	public abstract BaseContentTypes baseType();

	@Value.Default
	public String host() {
		return "SYSTEM_HOST";
	}

	@Value.Default
	public String folder() {
		return "SYSTEM_FOLDER";
	}

	public Permissionable permissionable() {
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

	public  List<Field> requiredFields(){
		return new ArrayList<Field>();
	}

}