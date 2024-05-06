package com.dotmarketing.business;

import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents a Role in the system. The dotCMS permission system enables you to control
 * user access to all dotCMS content and backend functionality through the use of both individual
 * user permissions and Roles assigned to each user. Roles in dotCMS can be configured to be flat
 * (where all Roles are top-level Roles that are independent from each other) or hierarchical.
 *
 * @author root
 * @since Mar 22nd, 2012
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Role implements Serializable,Comparable<Role> {

	private static final long serialVersionUID = 4882862821671404604L;
	
	public static final String SYSTEM = "System";
	@Deprecated
	/**
	 * Use CMS_ADMINISTRATOR_ROLE
	 */
	public static final String ADMINISTRATOR = "Administrator";
	@Deprecated
	public static final String POLLS_ADMIN = "Polls Admin";
	@Deprecated
	public static final String POWER_USER = "Power User";
	public static final String LOGIN_AS = "Login As";
	public static final String USER = "User";
	public static final String CMS_POWER_USER = "CMS Power User";
  public static final String CMS_ANONYMOUS_ROLE = "CMS Anonymous";
  public static final String CMS_OWNER_ROLE = "CMS Owner";
  public static final String CMS_ADMINISTRATOR_ROLE = "CMS Administrator";

  public static final String SCRIPTING_DEVELOPER = "Scripting Developer";

  
  
	public static final String DOTCMS_BACK_END_USER  = "DOTCMS_BACK_END_USER";
	public static final String DOTCMS_FRONT_END_USER = "DOTCMS_FRONT_END_USER";
	public static final String DBFQN_SEPARATOR = " --> ";

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
		return description!=null?description:"";
	}
	public void setDescription(String description) {
		this.description = description;
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

	/**
	 * This method is intended to sort the list of roles by the length of the {@code DB_FQN} value.
	 * This is because Roles are hierarchical, and the parents need to be imported before the
	 * children can be imported.
	 *
	 * @param role The {@link Role} object to be compared.
	 *
	 * @return If this object goes first, returns -1. If the Role object goes first, returns 1. If
	 * they are equal, returns 0.
	 */
	@Override
	public int compareTo(final Role role) {
		if (!this.getId().equals(role.getId())) {
			final List<RegExMatch> roleDBFQNElements = RegEX.find(role.getDBFQN(), DBFQN_SEPARATOR);
			final List<RegExMatch> thisDBFQNElements = RegEX.find(this.getDBFQN(), DBFQN_SEPARATOR);
			if (roleDBFQNElements.size() > thisDBFQNElements.size()) {
				return -1;
			} else if (roleDBFQNElements.size() < thisDBFQNElements.size()) {
				return 1;
			}
		}
		return 0;
	}
	
	@Override
	public boolean equals(final Object other) {
		if (!(other instanceof Role)) {
			return false;
		}

		final Role castOther = (Role) other;
		return (this.getId().equalsIgnoreCase(castOther.getId()));
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	public Map<String, Object> toMap() {
		Map<String, Object> roleMap = new HashMap<>();
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

	@Override
	public String toString() {
		return "Role{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				", description='" + description + '\'' +
				", roleKey='" + roleKey + '\'' +
				", DBFQN='" + DBFQN + '\'' +
				", FQN='" + FQN + '\'' +
				", parent='" + parent + '\'' +
				", editPermissions=" + editPermissions +
				", editUsers=" + editUsers +
				", editLayouts=" + editLayouts +
				", locked=" + locked +
				", system=" + system +
				'}';
	}

}
