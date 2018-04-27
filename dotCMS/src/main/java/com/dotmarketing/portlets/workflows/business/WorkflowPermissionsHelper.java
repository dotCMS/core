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

class WorkflowPermissionsHelper {

    private final PermissionAPI permissionAPI = APILocator.getPermissionAPI();

    private final RoleAPI roleAPI = APILocator.getRoleAPI();

    private final UserAPI userAPI = APILocator.getUserAPI();

    private volatile List<Role> specialRolesHierarchy;

    private final Role anyWhoCanViewContentRole;
    private final Role anyWhoCanEditContentRole;
    private final Role anyWhoCanPublishContentRole;
    private final Role anyWhoCanEditPermisionsContentRole;

    private final Role cmsAdminRole;

    WorkflowPermissionsHelper() throws DotDataException {
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
            return actions;
        }

        List<WorkflowAction> permissionables = new ArrayList<>(actions);
        if (permissionables.isEmpty()) {
            return permissionables;
        }

        final Iterator<WorkflowAction> actionsIterator = permissionables.iterator();
        while (actionsIterator.hasNext()) {
            final WorkflowAction action = actionsIterator.next();
            boolean hasPermission = false;
            if (null != permissionable) {
                // Validate if the action has one of the workflow special roles
                hasPermission = hasSpecialWorkflowPermission(user, respectFrontEndRoles,
                        permissionable, action);
            }
            // Validate if has other rolers permissions
            if (permissionAPI.doesUserHavePermission(action, PermissionAPI.PERMISSION_USE, user,
                    respectFrontEndRoles)) {
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
            return permissionables;
        }

        //First try to determine if we're dealing with a role directly mapped to a user.
        try {
            final User user = userAPI.loadUserById(role.getRoleKey());
            //if we're performing a filter on a role that is mapped to a user. Lets say Chris Publisher
            //for that reason we have a user, with assigned roles etc..
            return filterActions(permissionables, user, false, permissionable);
        } catch (Exception nsu) {
            Logger.debug(this, () -> "Unable to determine role belongs to a user.");
        }

        final Iterator<WorkflowAction> workflowActionIterator = permissionables.iterator();

        final List<Role> specialRolesHierarchy = getSpecialRolesByPrecedence();
        final int index = specialRolesHierarchy.indexOf(role);
        //Determine if we're dealing with a special role
        if (index >= 0) {
            //If so.. get a sublist preserving the precedence. so we can apply a filter with the resulting piece of hierarchy.
            final List<Role> rolesSublist = specialRolesHierarchy
                    .subList(index, specialRolesHierarchy.size());
            while (workflowActionIterator.hasNext()) {
                final WorkflowAction workflowAction = workflowActionIterator.next();
                //So special roles do not take into account a permissionable instance, they can only be applied on the action.
                //So this is an exact match lookup. The action has or doesn't have the special role. That's it.
                if (!hasAnyRolePermission(workflowAction, rolesSublist)) {
                    workflowActionIterator.remove();
                }
            }
            return permissionables;
        }

        //Perform filter for regular roles
        final Set<Role> roles = collectChildRoles(role);
        while (workflowActionIterator.hasNext()) {
            final WorkflowAction workflowAction = workflowActionIterator.next();
            if (!hasAnyRolePermission(workflowAction, roles, permissionable)) {
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
     * @param roles the roles to apply
     * @return true if any of the roles is met by the action
     */
    private boolean hasAnyRolePermission(final WorkflowAction workflowAction, final Iterable<Role> roles)
            throws DotDataException {
        for (final Role role : roles) {
            if (isRolePresent(workflowAction, role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Given a set roles an action and a permissionable. This method verifies that at least one of
     * the roles is present on the actions and the permissionable
     */
    private boolean hasAnyRolePermission(final WorkflowAction workflowAction,
            final Iterable<Role> collectedRoles, final Permissionable permissionable)
            throws DotDataException {
        //When no permissionable is available we simply rely on the validation of the role being present on the action.
        if (permissionable == null) {
            return hasAnyRolePermission(workflowAction, collectedRoles);
        }
        //if there's a permissionable we need to perform a double check.
        //We need to make sure the role is present on the action and on the permissionable.
        for (final Role role : collectedRoles) {
            return (isRolePresent(workflowAction, role) && isRolePresent(permissionable, role));
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
            Permissionable permissionable, WorkflowAction action) throws DotDataException {

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
     * Return true if the user have over the permissionable the specified permission.
     *
     * @param permissionable the ContentType or contentlet to validate
     * @param permissiontype The type of permission to validate
     * @param user The User over who the permissions are going to be validate
     * @param respectFrontEndRoles boolean indicating if the frontend roles should be repected
     * @return true if the user have permissions, false if not
     */
    @CloseDBIfOpened
    private boolean doesUserHavePermission(final Permissionable permissionable,
            final int permissiontype, final User user, final boolean respectFrontEndRoles) throws DotDataException {
        if (permissionable instanceof Contentlet && !InodeUtils
                .isSet(permissionable.getPermissionId())) {
            if (permissionAPI.doesUserHavePermission(((Contentlet) permissionable).getContentType(),
                    permissiontype, user, respectFrontEndRoles)) {
                return true;
            }
        } else {
            if (permissionAPI.doesUserHavePermission(permissionable, permissiontype, user,
                    respectFrontEndRoles)) {
                return true;
            }
        }
        return false;
    }

    private List<Role> getSpecialRolesByPrecedence() throws DotDataException {
        return specialRolesHierarchy;
    }


}
