package com.dotcms.contenttype.model.type;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;

import org.immutables.value.Value;
import org.immutables.value.Value.Default;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.org.apache.commons.lang.time.DateUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.RelatedPermissionableGroup;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.fasterxml.jackson.annotation.JsonIgnore;



public abstract class ContentType implements Serializable, Permissionable,ContentTypeIf {
	
	
	
	@Value.Check
	protected void check() {
		if(!(this instanceof UrlMapable)){
			Preconditions.checkArgument(detailPage()==null,"Detail Page cannot be set for " + this.getClass());
			Preconditions.checkArgument(urlMapPattern()==null,"urlmap cannot be set for " + this.getClass());
		}
		if(!(this instanceof Expireable)){
			Preconditions.checkArgument(expireDateVar()==null,"expireDateVar cannot be set for " + this.getClass());
			Preconditions.checkArgument(publishDateVar()==null,"publishDateVar cannot be set for " + this.getClass());
		}
	}

	static final long serialVersionUID = 1L;

	public abstract String name();

	@Nullable
	public abstract String inode();

	@Nullable
	public abstract String description();

	@Value.Default
	public boolean defaultType() {
		return false;
	}
	@JsonIgnore
	@Value.Default
	public StorageType storageType(){
		return  ImmutableDbStorageType.of();
	}

    @Value.Default
    @Nullable
    public String detailPage(){
        return null;
    }
    
	@Value.Default
	public boolean fixed() {
		return false;
	}

	@Value.Default
	public Date iDate() {
		return DateUtils.round(new Date(), Calendar.SECOND);
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
	
	
	public abstract String variable();
	@Nullable
	@Value.Default
	public  String urlMapPattern(){
	    return null;
	}
	@Nullable
	@Value.Default
	public  String publishDateVar(){
	    return null;
	}
	@Nullable
	@Value.Default
	public  String expireDateVar(){
	    return null;
	}
	@Nullable
	@Value.Default
	public String owner(){
	    return null;
	}

	@Value.Default
	public Date modDate() {
		return DateUtils.round(new Date(), Calendar.SECOND);
	}

	public abstract BaseContentType baseType();

	@Value.Default
	public String host() {
		return Host.SYSTEM_HOST;
	}
	
	@JsonIgnore
	@Value.Lazy
	public  List<Field> fields(){
		try {
			//System.out.println("loading fields:" + this);
			return APILocator.getFieldAPI2().byContentTypeId(this.inode());
		} catch (DotDataException e) {
			throw new DotStateException("unable to load fields:"  +e.getMessage(), e);
		}
	}
	
	@Value.Default
	public String folder() {
		return Folder.SYSTEM_FOLDER;
	}

	public Permissionable permissionable() {
		return this;
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
	@JsonIgnore
	@Override
	public List<PermissionSummary> acceptedPermissions() {
		return ImmutableList.of(new PermissionSummary("view", "view-permission-description", PermissionAPI.PERMISSION_READ),
				new PermissionSummary("edit", "edit-permission-description", PermissionAPI.PERMISSION_WRITE), new PermissionSummary(
						"publish", "publish-permission-description", PermissionAPI.PERMISSION_PUBLISH), new PermissionSummary(
						"edit-permissions", "edit-permissions-permission-description", PermissionAPI.PERMISSION_EDIT_PERMISSIONS));

	}
	@JsonIgnore
	@Value.Lazy
	public Permissionable getParentPermissionable()  {
		try {
			Permissionable parent =  (FolderAPI.SYSTEM_FOLDER.equals(this.folder())) 
					? APILocator.getHostAPI().find(this.host(), APILocator.systemUser(), false)
							: APILocator.getFolderAPI().find(this.folder(), APILocator.systemUser(), false);
					
			
			return parent;
		} catch (Exception e) {
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public boolean isParentPermissionable() {
		return true;
	}
	@JsonIgnore
	@Override
	public List<RelatedPermissionableGroup> permissionDependencies(int requiredPermission) {
		return null;
	}

	@Override
	public String getPermissionType() {
		return Structure.class.getCanonicalName();
	}
	@JsonIgnore
	@Default
	public  List<Field> requiredFields(){
		return ImmutableList.of();
	}

}