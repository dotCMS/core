package com.dotmarketing.portlets.workflows.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.BooleanUtils;

class WorkflowActionUtils {

    static final boolean RESPECT_FRONTEND_ROLES = true;

    private final PermissionAPI permissionAPI = APILocator.getPermissionAPI();

    private final RoleAPI roleAPI = APILocator.getRoleAPI();

    private final UserAPI userAPI = APILocator.getUserAPI();

    private final List<Role> specialRolesHierarchy;

    private final Role anyWhoCanViewContentRole;
    private final Role anyWhoCanEditContentRole;
    private final Role anyWhoCanPublishContentRole;
    private final Role anyWhoCanEditPermisionsContentRole;

    private final Role cmsAdminRole;

    WorkflowActionUtils() throws DotDataException {
        anyWhoCanViewContentRole = roleAPI
                .loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_VIEW_ROLE_KEY);
        anyWhoCanEditContentRole = roleAPI
                .loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_ROLE_KEY);
        anyWhoCanPublishContentRole = roleAPI
                .loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_PUBLISH_ROLE_KEY);
        anyWhoCanEditPermisionsContentRole = roleAPI
                .loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_PERMISSIONS_ROLE_KEY);
        // Do not alter the order of the Elements on the list. It is tied to the logic!!
        specialRolesHierarchy = ImmutableList
                .of(anyWhoCanEditPermisionsContentRole, anyWhoCanPublishContentRole,
                        anyWhoCanEditContentRole, anyWhoCanViewContentRole);

        cmsAdminRole = roleAPI.loadCMSAdminRole();
    }


    /**
     * Filter the list of actions to display according to the user logged permissions
     *
     * @param actions List of action to filter
     * @param user User to validate
     * @param respectFrontEndRoles indicates if should respect frontend roles
     * @param permissionable ContentType or contentlet to validate special workflow roles
     * @return List<WorkflowAction>
     */
    @CloseDBIfOpened
    List<WorkflowAction> filterActions(final List<WorkflowAction> actions,
            final User user, final boolean respectFrontEndRoles,
            final Permissionable permissionable) throws DotDataException {

        if ((user != null) && roleAPI.doesUserHaveRole(user, cmsAdminRole)) {
            Logger.debug(this, () -> "user:" + user.getUserId()
                    + " has an admin role. returning all actions.");
            return actions;
        }

        final List<WorkflowAction> permissionables = new ArrayList<>(actions);
        if (permissionables.isEmpty()) {
            Logger.debug(this, () -> " No actions were passed. ");
            return permissionables;
        }

        final Iterator<WorkflowAction> actionsIterator = permissionables.iterator();
        while (actionsIterator.hasNext()) {
            final WorkflowAction action = actionsIterator.next();
            boolean hasPermission = false;
            if (null != permissionable) {
                // Validate if the action has one of the workflow special roles
                final boolean doesHavePermission = hasSpecialWorkflowPermission(user,
                        respectFrontEndRoles, permissionable, action);
                Logger.debug(this, () -> " Trying special roles for action " + action.getName()
                        + " had permissions:" + BooleanUtils.toStringYesNo(doesHavePermission));
                hasPermission = doesHavePermission;
            }
            // Validate if has other role permissions
            if (doesUserHavePermission(action, PermissionAPI.PERMISSION_USE, user,
                    respectFrontEndRoles)) {
                Logger.debug(this, () -> " Trying other roles for action " + action.getName()
                        + " had permissions: yes");
                hasPermission = true;
            }

            if (!hasPermission) {
                actionsIterator.remove();
            }
        }

        return permissionables;
    }

    /**
     * Filter the list of actions to display according to the user logged permissions
     *
     * @param actions List of action to filter
     * @param user User to validate
     * @param respectFrontEndRoles indicates if should respect frontend roles
     * @return List<WorkflowAction>
     */
    @CloseDBIfOpened
    List<WorkflowAction> filterBulkActions(final List<WorkflowAction> actions,
            final User user, final boolean respectFrontEndRoles) throws DotDataException {

        if ((user != null) && roleAPI.doesUserHaveRole(user, cmsAdminRole)) {
            Logger.debug(this, () -> "user:" + user.getUserId()
                    + " has an admin role. returning all actions.");
            return actions;
        }

        final List<WorkflowAction> permissionables = new ArrayList<>(actions);
        if (permissionables.isEmpty()) {
            Logger.debug(this, () -> " No actions were passed. ");
            return permissionables;
        }

        final Iterator<WorkflowAction> actionsIterator = permissionables.iterator();
        while (actionsIterator.hasNext()) {

            final WorkflowAction action = actionsIterator.next();
            boolean hasPermission = false;

            // Validate if the action has user/role permissions
            if (doesUserHavePermission(action, PermissionAPI.PERMISSION_USE, user,
                    respectFrontEndRoles)) {
                Logger.debug(this, () -> " Trying other roles for action " + action.getName()
                        + " had permissions: yes");
                hasPermission = true;
            }

            /*
            If we don't have direct permissions over the action but the action has special roles
            our best guess is to allow the user to try to execute the action in the bulk actions modal,
            at this point and the way we calculate the bulk operations for efficiency we don't have
            a permissionable to use in order to validate individual permissions.
             */
            if (!hasPermission && hasSpecialWorkflowRoles(action)) {
                hasPermission = true;
            }

            if (!hasPermission) {
                actionsIterator.remove();
            }
        }

        return permissionables;
    }

    /**
     * Filters the list of actions to display according to the role passed
     *
     * @param actions the actions to filter by role
     * @param role the role selected
     * @return the list of actions once the filter operation has taken place
     */
    @CloseDBIfOpened
    List<WorkflowAction> filterActions(final List<WorkflowAction> actions, final Role role,
            final Permissionable permissionable) throws DotDataException {

        final List<WorkflowAction> permissionables = new ArrayList<>(actions);
        if (permissionables.isEmpty()) {
            Logger.debug(this, () -> " No actions were passed. " );
            return permissionables;
        }

        //First try to determine if we're dealing with a role directly mapped to a user.
        try {
            final User user = userAPI.loadUserById(role.getRoleKey());
            //if we're performing a filter on a role that is mapped to a user. Lets say Chris Publisher
            //for that reason we have a user, with assigned roles etc..
            Logger.debug(this, () -> " Role :" + role.getName() + " is mapped to a user, filtering based on user permissions." );
            return filterActions(permissionables, user, !RESPECT_FRONTEND_ROLES, permissionable);
        } catch (Exception nsu) {
            Logger.debug(this, () -> "Unable to determine role belongs to a user.");
        }

        if ((role != null) && (role.equals(cmsAdminRole) || cmsAdminRole.getRoleKey()
                .equals(role.getRoleKey()))) {
            Logger.debug(this, () -> "Admin role. returning all actions.");
            return actions;
        }

        final Iterator<WorkflowAction> workflowActionIterator = permissionables.iterator();

        final List<Role> specialRolesHierarchy = getSpecialRolesByPrecedence();
        final int index = specialRolesHierarchy.indexOf(role);
        //Determine if we're dealing with a special role
        if (index >= 0) {
            Logger.debug(this, () -> " Filtering by special roles." );
            //If so.. get a sublist preserving the precedence. so we can apply a filter with the resulting piece of hierarchy.
            final List<Role> rolesSublist = specialRolesHierarchy
                    .subList(index, specialRolesHierarchy.size());
            while (workflowActionIterator.hasNext()) {
                final WorkflowAction workflowAction = workflowActionIterator.next();
                //So special roles do not take into account a permissionable instance, they can only be applied on the action.
                //So this is an exact match lookup. The action has or doesn't have the special role. That's it.
                if (!isAnyRolePresent(workflowAction, rolesSublist)) {
                    workflowActionIterator.remove();
                }
            }
            return permissionables;
        }

        //Perform filter for regular roles
        Logger.debug(this, () -> " Filtering by regular roles. " );
        final Set<Role> collectedRoles = collectChildRoles(role);
        while (workflowActionIterator.hasNext()) {
            final WorkflowAction workflowAction = workflowActionIterator.next();
            if (!isAnyRolePresent(workflowAction, collectedRoles, permissionable)) {
                workflowActionIterator.remove();
            }
        }

        return permissionables;
    }

    /**
     * Gathers all child roles recursively.
     *
     * @param selectedRole parent role passed
     * @return returns a set of roles including the one initially passed as parameter.
     */
    private Set<Role> collectChildRoles(final Role selectedRole) {
        final Set<Role> collectedRoles = new HashSet<>();
        collectedRoles.add(selectedRole);
        try {
            final List<String> childrenIds = selectedRole.getRoleChildren();
            if (UtilMethods.isSet(childrenIds)) {
                for (final String childRoleId : childrenIds) {
                    final Role childRole = roleAPI.loadRoleById(childRoleId);
                    collectedRoles.addAll(
                            collectChildRoles(childRole)
                    );
                }
            }
        } catch (Exception e) {
            Logger.error(this, "Error collecting children roles ", e);
        }

        return collectedRoles;
    }

    /**
     * Given a set roles this method verifies if an action has access to at least one of them.
     *
     * @param workflowAction the workflow action to examine
     * @param collectedRoles the roles to apply
     * @return true if any of the roles is met by the action
     */
    private boolean isAnyRolePresent(final WorkflowAction workflowAction, final Iterable<Role> collectedRoles)
            throws DotDataException {
        for (final Role role : collectedRoles) {
            if (isRolePresent(workflowAction, role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Given a set roles an action and a permissionable. This method verifies that at least one of
     * the roles is present. But the role must be resent in both the actions and the permissionable instance.
     */
    private boolean isAnyRolePresent(final WorkflowAction workflowAction,
            final Iterable<Role> collectedRoles, final Permissionable permissionable)
            throws DotDataException {
        //When no permissionable is available we simply rely on the validation of the role being present on the action.
        if (permissionable == null) {
            return isAnyRolePresent(workflowAction, collectedRoles);
        }
        //if there's a permissionable we need to perform a double check.
        //We need to make sure the role is present on the action and on the permissionable.
        for (final Role role : collectedRoles) {
            if ((isRolePresent(workflowAction, role) && isRolePresent(permissionable, role))
                    || hasSpecialWorkflowPermission(role, !RESPECT_FRONTEND_ROLES, permissionable,
                    workflowAction)) {
                return true;
            }
        }
        return false;
    }


    private boolean isRolePresent(final Permissionable permissionable, final Role role)
            throws DotDataException {
        return permissionAPI
                .doesRoleHavePermission(permissionable, PermissionAPI.PERMISSION_USE, role);
    }

    /**
     * Return true if the action has one of the workflow action roles and if the user has  any of
     * those permission over the content or content type
     *
     * @param user User to validate
     * @param respectFrontEndRoles indicates if should respect frontend roles
     * @param permissionable ContentType or contentlet to validate special workflow roles
     * @param action The action to validate
     * @return true if the user has one of the special workflow action role permissions, false if
     * not
     */
    @CloseDBIfOpened
    boolean hasSpecialWorkflowPermission(final User user, final boolean respectFrontEndRoles,
            final Permissionable permissionable, final WorkflowAction action) throws DotDataException {

        if (isRolePresent(action, anyWhoCanViewContentRole)) {
            return doesUserHavePermission(permissionable,
                    PermissionAPI.PERMISSION_READ, user, respectFrontEndRoles);
        }
        if (isRolePresent(action, anyWhoCanEditContentRole)) {
            return doesUserHavePermission(permissionable,
                    PermissionAPI.PERMISSION_WRITE, user, respectFrontEndRoles);
        }
        if (isRolePresent(action, anyWhoCanPublishContentRole)) {
            return doesUserHavePermission(permissionable,
                    PermissionAPI.PERMISSION_PUBLISH, user, respectFrontEndRoles);
        }
        if (isRolePresent(action, anyWhoCanEditPermisionsContentRole)) {
            return doesUserHavePermission(permissionable,
                    PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user, respectFrontEndRoles);
        }
        return false;
    }

    /**
     * Verifies if the given Workflow Action has special roles on it
     */
    @CloseDBIfOpened
    private boolean hasSpecialWorkflowRoles(final WorkflowAction action) throws DotDataException {

        return isRolePresent(action, anyWhoCanViewContentRole)
                || isRolePresent(action, anyWhoCanEditContentRole)
                || isRolePresent(action, anyWhoCanPublishContentRole)
                || isRolePresent(action, anyWhoCanEditPermisionsContentRole);
    }

    /**
     * Return true if the user have over the permissionable the specified permission.
     *
     * @param permissionable the ContentType or contentlet to validate
     * @param permissionType The type of permission to validate
     * @param user The User over who the permissions are going to be validate
     * @param respectFrontEndRoles boolean indicating if the frontend roles should be repected
     * @return true if the user have permissions, false if not
     */
    @CloseDBIfOpened
    private boolean doesUserHavePermission(final Permissionable permissionable,
            final int permissionType, final User user, final boolean respectFrontEndRoles) throws DotDataException {
        if (permissionable instanceof Contentlet && !InodeUtils
                .isSet(permissionable.getPermissionId())) {
            if (permissionAPI.doesUserHavePermission(((Contentlet) permissionable).getContentType(),
                    permissionType, user, respectFrontEndRoles)) {
                return true;
            }
        } else {
            if (permissionAPI.doesUserHavePermission(permissionable, permissionType, user,
                    respectFrontEndRoles)) {
                return true;
            }
        }
        return false;
    }

    @CloseDBIfOpened
    boolean hasSpecialWorkflowPermission(final Role role, final boolean respectFrontEndRoles,
            final Permissionable permissionable, final WorkflowAction action) throws DotDataException {

        if (isRolePresent(action, anyWhoCanViewContentRole)) {
            return doesRoleHavePermission(permissionable,
                    PermissionAPI.PERMISSION_READ, role, respectFrontEndRoles);
        }
        if (isRolePresent(action, anyWhoCanEditContentRole)) {
            return doesRoleHavePermission(permissionable,
                    PermissionAPI.PERMISSION_WRITE, role, respectFrontEndRoles);
        }
        if (isRolePresent(action, anyWhoCanPublishContentRole)) {
            return doesRoleHavePermission(permissionable,
                    PermissionAPI.PERMISSION_PUBLISH, role, respectFrontEndRoles);
        }
        if (isRolePresent(action, anyWhoCanEditPermisionsContentRole)) {
            return doesRoleHavePermission(permissionable,
                    PermissionAPI.PERMISSION_EDIT_PERMISSIONS, role, respectFrontEndRoles);
        }
        return false;
    }

    @CloseDBIfOpened
    private boolean doesRoleHavePermission(final Permissionable permissionable,
            final int permissionType, final Role role, final boolean respectFrontEndRoles) throws DotDataException {
        if (permissionable instanceof Contentlet && !InodeUtils
                .isSet(permissionable.getPermissionId())) {
            if (permissionAPI.doesRoleHavePermission(((Contentlet) permissionable).getContentType(),
                    permissionType, role, respectFrontEndRoles)) {
                return true;
            }
        } else {
            if (permissionAPI.doesRoleHavePermission(permissionable, permissionType, role,
                    respectFrontEndRoles)) {
                return true;
            }
        }
        return false;
    }

    private List<Role> getSpecialRolesByPrecedence() {
        return specialRolesHierarchy;
    }

}
