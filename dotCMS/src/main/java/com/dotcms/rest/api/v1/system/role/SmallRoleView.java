package com.dotcms.rest.api.v1.system.role;

/**
 * Encapsulates a few info of the role
 * @author jsanca
 */
public class SmallRoleView {

    private String name;
    private String id;
    private String roleKey;
	private boolean user;

    public SmallRoleView(String name, String id, String roleKey, boolean user) {
        this.name = name;
        this.id = id;
        this.roleKey = roleKey;
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getRoleKey() {
        return roleKey;
    }

    public boolean isUser() {
        return user;
    }
}
