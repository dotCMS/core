package com.dotcms.publisher.environment.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.RelatedPermissionableGroup;
import com.dotmarketing.exception.DotDataException;

public class Environment implements Permissionable,Serializable{

	// Beginning Permissionable methods


	public String getPermissionId() {
		return this.getId();
	}

	public String getOwner() {
		return null;
	}
	public void setOwner(String owner) {
	}

	@JsonIgnore
	public List<PermissionSummary> acceptedPermissions() {
		List<PermissionSummary> accepted = new ArrayList<PermissionSummary>();
		accepted.add(new PermissionSummary("use",
				"use-permission-description", PermissionAPI.PERMISSION_USE));
		return accepted;
	}
	@JsonIgnore
	public List<RelatedPermissionableGroup> permissionDependencies(int requiredPermission) {
		return null;
	}
	@JsonIgnore
	public Permissionable getParentPermissionable() throws DotDataException {
		return null;
	}
	@JsonIgnore
	public String getPermissionType() {
		return this.getClass().getCanonicalName();
	}
	@JsonIgnore
	public boolean isParentPermissionable() {
		return false;
	}

	// End Permissionable methods

	private static final long serialVersionUID = -8154524665918330146L;

	private String id;
	private String name;
	private Boolean pushToAll;

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Boolean getPushToAll() {
		return pushToAll;
	}
	public void setPushToAll(Boolean pushToAll) {
		this.pushToAll = pushToAll;
	}


	@Override
	public int hashCode() {		
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Environment))
			return false;
		else {
			Environment e = (Environment)obj;
			return this.id.equalsIgnoreCase(e.getId());
		}
	}
	

}

