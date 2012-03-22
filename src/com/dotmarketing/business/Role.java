package com.dotmarketing.business;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;

public class Role implements Serializable,Comparable<Role> {

	/**
	 * @author Jason Tesser
	 */
	private static final long serialVersionUID = 4882862821671404604L;
	
	public static final String SYSTEM = "System";
	public static final String ADMINISTRATOR = "Administrator";
	public static final String POLLS_ADMIN = "Polls Admin";
	public static final String POWER_USER = "Power User";
	public static final String LOGIN_AS = "Login As";
	public static final String USER = "User";
	public static final String CMS_POWER_USER = "CMS Power User";
	
	private String id = "";
	private String name;
	private String description;
	private String roleKey = null;
	private String DBFQN;
	private String FQN;
	private String parent;
	private boolean editPermissions = true;
	private boolean editUsers = true;
	private boolean editLayouts = true;
	private boolean locked = false;
	private boolean system = false;
	private List<String> roleChildren;
	
	public Role() {
	
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
		if(!UtilMethods.isSet(parent)){
			parent = id;
		}
		if(!UtilMethods.isSet(DBFQN)){
			DBFQN = id;
		}
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String descriptin) {
		this.description = descriptin;
	}
	public String getRoleKey() {
		return roleKey;
	}
	public void setRoleKey(String roleKey) {
		this.roleKey = roleKey;
	}
	public String getParent() {
		return parent;
	}
	public void setParent(String parent) {
		this.parent = parent;
	}

	public List<String> getRoleChildren() {
		return roleChildren;
	}

	public void setRoleChildren(List<String> roleChildren) {
		this.roleChildren = roleChildren;
	}

	public boolean isSystem() {
		return system;
	}

	public void setSystem(boolean system) {
		this.system = system;
	}

	public String getFQN() {
		return FQN;
	}

	public void setFQN(String fQN) {
		FQN = fQN;
	}

	public String getDBFQN() {
		return DBFQN;
	}

	public void setDBFQN(String dBFQN) {
		DBFQN = dBFQN;
	}

	public boolean isEditPermissions() {
		return editPermissions;
	}

	public void setEditPermissions(boolean editPermissions) {
		this.editPermissions = editPermissions;
	}

	public boolean isEditUsers() {
		return editUsers;
	}

	public void setEditUsers(boolean editUsers) {
		this.editUsers = editUsers;
	}

	public boolean isEditLayouts() {
		return editLayouts;
	}

	public void setEditLayouts(boolean editLayouts) {
		this.editLayouts = editLayouts;
	}

	public boolean isLocked() {
		return locked;
	}
	public boolean isUser() {
		return (this.getFQN() != null && this.getFQN().startsWith("User"));
	}
	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	
	// -1  means this first
	// 1 means Role first
	// 0 equal
	public int compareTo(Role o) {
		if (!(o instanceof Role))
			return -1;
		Role role = (Role) o;
		if(this.getParent().equals(this.getId())){
			return -1;
		}
		if(o.getId().equals(o.getParent())){
			return 1;
		}
		if(RegEX.find(role.getDBFQN(), " --> ").size() > RegEX.find(this.getDBFQN(), " --> ").size()){
			return -1;
		}else if(RegEX.find(role.getDBFQN(), " --> ").size() < RegEX.find(this.getDBFQN(), " --> ").size()){
			return 1;
		}else{
			return 0;
		}
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Role)) {
			return false;
		}

		Role castOther = (Role) other;
		
		return (this.getId().equalsIgnoreCase(castOther.getId()));
	}
	
	public Map<String, Object> toMap() {
		Map<String, Object> roleMap = new HashMap<String, Object>();
		roleMap.put("DBFQN", this.DBFQN);
		roleMap.put("description", this.description);
		roleMap.put("editLayouts", this.editLayouts);
		roleMap.put("editPermissions", this.editPermissions);
		roleMap.put("editUsers", this.editUsers);
		roleMap.put("FQN", this.FQN);
		roleMap.put("id", this.id);
		roleMap.put("locked", this.locked);
		roleMap.put("name", this.name);
		roleMap.put("parent", this.parent);
		roleMap.put("roleKey", this.roleKey);
		roleMap.put("system", this.system);
		return roleMap;
	}
	
}
