package com.dotmarketing.business;


public class UsersRoles {

	private String id;
	private String userId;
	private String roleId;
	
	public UsersRoles() {}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof UsersRoles)) {
			return false;
		}

		UsersRoles castOther = (UsersRoles) other;

		return (this.getId().equalsIgnoreCase(castOther.getId()));
	}
}
