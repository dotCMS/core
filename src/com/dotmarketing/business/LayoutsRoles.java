package com.dotmarketing.business;

import java.io.Serializable;


public class LayoutsRoles implements Serializable {

	private static final long serialVersionUID = 244359427451632900L;
	
	private String id;
	private String layoutId;
	private String roleId;
	
	public LayoutsRoles() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof LayoutsRoles)) {
			return false;
		}

		LayoutsRoles castOther = (LayoutsRoles) other;

		return (this.getId().equalsIgnoreCase(castOther.getId()));
	}

	public String getLayoutId() {
		return layoutId;
	}

	public void setLayoutId(String layoutId) {
		this.layoutId = layoutId;
	}
}
