package com.dotcms.rest.api.v1.user;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;

/**
 * Helper to group SAML and User Resource common methods.
 */
public class UserHelper {


    private static class SingletonHolder {
        private static final UserHelper INSTANCE = new UserHelper();
    }

    /**
     * Get the instance.
     * @return UserHelper
     */
    public static UserHelper getInstance() {

        return UserHelper.SingletonHolder.INSTANCE;
    } // getInstance.

    private final RoleAPI roleAPI;

    @VisibleForTesting
    public UserHelper() {
        this(APILocator.getRoleAPI());
    }

    @VisibleForTesting
    public UserHelper(final RoleAPI roleAPI) {
        this.roleAPI = roleAPI;
    }


    /**
     * Remove the roles associated to the user
     * @param user User
     */
    public void removeRoles(User user) throws DotDataException {

        Logger.debug(this, ()-> "removing the roles for the user:" + user.getUserId());
        this.roleAPI.removeAllRolesFromUser(user);
    }


    /**
     * Adds a new role to the user.
     * @param user {@link User} user to add the role.
     * @param roleKey {@link String} role key to add.
     * @param createRole {@link Boolean} create the role if it does not exist.
     * @param isSystem {@link Boolean} if it is system role
     * @throws DotDataException
     */
    public void addRole(final User user, final String roleKey, final boolean createRole, final boolean isSystem)
            throws DotDataException {

        Role role = this.roleAPI.loadRoleByKey(roleKey);

        // create the role, in case it does not exist
        if (role == null && createRole) {
            Logger.info(this, "Role with key '" + roleKey + "' was not found. Creating it...");
            role = createNewRole(roleKey, isSystem);
        }

        if (null != role) {
            if (!this.roleAPI.doesUserHaveRole(user, role)) {
                this.roleAPI.addRoleToUser(role, user);
                Logger.debug(this, "Role named '" + role.getName() + "' has been added to user: " + user.getEmailAddress());
            } else {
                Logger.debug(this,
                        "User '" + user.getEmailAddress() + "' already has the role '" + role + "'. Skipping assignment...");
            }
        } else {
            Logger.debug(this, "Role named '" + roleKey + "' does NOT exists in dotCMS. Ignoring it...");
        }
    }

    /**
     * Creates a new role.
     * @param roleKey {@link String} role key
     * @param isSystem {@link Boolean} if it is system role
     * @return
     * @throws DotDataException
     */
    public Role createNewRole(final String roleKey, final boolean isSystem) throws DotDataException {

        Role role = new Role();
        role.setName(roleKey);
        role.setRoleKey(roleKey);
        role.setEditUsers(true);
        role.setEditPermissions(true);
        role.setEditLayouts(true);
        role.setDescription("");
        role.setId(UUIDGenerator.generateUuid());

        final String date = DateUtil.getCurrentDate();

        ActivityLogger.logInfo(ActivityLogger.class, getClass() + " - Adding Role",
                "Date: " + date + "; " + "Role:" + roleKey);
        AdminLogger.log(AdminLogger.class, getClass() + " - Adding Role", "Date: " + date + "; " + "Role:" + roleKey);

        try {
            role = roleAPI.save(role, role.getId());
        } catch (DotDataException | DotStateException e) {
            ActivityLogger.logInfo(ActivityLogger.class, getClass() + " - Error adding Role",
                    "Date: " + date + ";  " + "Role:" + roleKey);
            AdminLogger.log(AdminLogger.class, getClass() + " - Error adding Role",
                    "Date: " + date + ";  " + "Role:" + roleKey);
            throw e;
        }

        return role;
    }
}
