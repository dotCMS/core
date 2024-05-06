package com.dotcms.rendering.js.proxy;

import com.dotmarketing.business.Role;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyHashMap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsRole implements Serializable, Comparable<JsRole>, JsProxyObject<Role> {

	private final Role role;

	public JsRole(final Role role) {
		this.role = role;
	}

	/**
	 * Returns the actual role, but can not be retrieved on the JS context.
	 * @return
	 */
	public Role getRole() {
		return role;
	}

	@Override
	public Role  getWrappedObject() {
		return this.getRole();
	}

	@HostAccess.Export
	public String getId() {
		return this.role.getId();
	}

	@HostAccess.Export
	public String getName() {
		return this.role.getName();
	}

	@HostAccess.Export
	public String getDescription() {
		return this.role.getDescription();
	}

	@HostAccess.Export
	public String getRoleKey() {
		return this.role.getRoleKey();
	}

	@HostAccess.Export
	public String getParent() {
		return this.role.getParent();
	}

	@HostAccess.Export
	public List<String> getRoleChildren() {
		return this.role.getRoleChildren();
	}


	@HostAccess.Export
	public boolean isSystem() {
		return this.role.isSystem();
	}

	@HostAccess.Export
	public String getFQN() {
		return this.role.getFQN();
	}

	@HostAccess.Export
	public String getDBFQN() {
		return this.role.getDBFQN();
	}

	@HostAccess.Export
	public boolean isEditPermissions() {
		return this.role.isEditPermissions();
	}

	@HostAccess.Export
	public boolean isEditUsers() {
		return this.role.isEditUsers();
	}

	@HostAccess.Export
	public boolean isEditLayouts() {
		return this.role.isEditLayouts();
	}

	@HostAccess.Export
	public boolean isLocked() {
		return this.role.isLocked();
	}

	@HostAccess.Export
	public boolean isUser() {
		return this.role.isUser();
	}

	@HostAccess.Export
	// -1  means this first
	// 1 means Role first
	// 0 equal
	public int compareTo(final JsRole inputJsRole) {

		if (!(inputJsRole instanceof JsRole)) {
			return -1;
		}

		return this.getWrappedObject().compareTo(inputJsRole.getWrappedObject());
	}

	@HostAccess.Export
	@Override
	public boolean equals(final Object other) {

		if (!(other instanceof JsRole)) {

			return false;
		}

		final JsRole castOther = (JsRole) other;
		return this.getWrappedObject().equals(castOther.getWrappedObject());
	}

	@HostAccess.Export
	@Override
	public int hashCode() {
		return this.role.getId().hashCode();
	}

	@HostAccess.Export
	public ProxyHashMap toMap() {

		final Map<String, Object> roleMap = new HashMap<>();
		roleMap.put("DBFQN", this.getDBFQN());
		roleMap.put("description", this.getDescription());
		roleMap.put("editLayouts", this.isEditLayouts());
		roleMap.put("editPermissions", this.isEditPermissions());
		roleMap.put("editUsers", this.isEditUsers());
		roleMap.put("FQN", this.getFQN());
		roleMap.put("id", this.getId());
		roleMap.put("locked", this.isLocked());
		roleMap.put("name", this.getName());
		roleMap.put("parent", this.getParent());
		roleMap.put("roleKey", this.getRoleKey());
		roleMap.put("system", this.isSystem());
		return ProxyHashMap.from((Map)roleMap);
	}

	@HostAccess.Export
	@Override
	public String toString() {
		return "Role{" +
				"id='" + this.getId() + '\'' +
				", name='" + this.getName() + '\'' +
				", description='" + this.getDescription() + '\'' +
				", roleKey='" + this.getRoleKey() + '\'' +
				", DBFQN='" + this.getDBFQN() + '\'' +
				", FQN='" + this.getFQN() + '\'' +
				", parent='" + this.getParent() + '\'' +
				", editPermissions=" + this.isEditPermissions() +
				", editUsers=" + this.isEditUsers() +
				", editLayouts=" + this.isEditLayouts() +
				", locked=" + this.isLocked() +
				", system=" + this.isSystem() +
				'}';
	}
}
