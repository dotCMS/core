package com.dotmarketing.portlets.workflows.ajax;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import java.util.Set;

/**
 * Decides which back-end users may read the role, step and assignee lists that the legacy
 * workflow, publishing and configuration screens load through the workflow ajax actions.
 */
final class WorkflowAjaxReadAccess {

    /**
     * Portlets whose screens use the role picker backed by {@link WfRoleStoreAjax}: the scheme
     * action editor ({@code workflow-schemes}), the Workflow Tasks "Assigned to" filter
     * ({@code workflow}), the publishing environment dialog ({@code configuration}) and the
     * bundle editor ({@code publishing-queue}).
     */
    static final Set<String> ROLE_PICKER_PORTLETS = Set.of(
            PortletID.WORKFLOW_SCHEMES.toString(), PortletID.WORKFLOW.toString(),
            PortletID.CONFIGURATION.toString(), PortletID.PUBLISHING_QUEUE.toString());

    /**
     * Portlets whose screens list the steps of a scheme through {@link WfStepAjax}: Workflow Tasks
     * ({@code workflow}) and Workflow Schemes ({@code workflow-schemes}).
     */
    static final Set<String> STEP_LIST_PORTLETS = Set.of(
            PortletID.WORKFLOW.toString(), PortletID.WORKFLOW_SCHEMES.toString());

    private final LayoutAPI layoutAPI;
    private final PermissionAPI permissionAPI;
    private final RoleAPI roleAPI;

    /**
     * Creates the access check with the APIs from {@link APILocator}.
     */
    WorkflowAjaxReadAccess() {
        this(APILocator.getLayoutAPI(), APILocator.getPermissionAPI(), APILocator.getRoleAPI());
    }

    /**
     * Creates the access check with the given APIs.
     *
     * @param layoutAPI     resolves the portlets a user can open
     * @param permissionAPI resolves the USE permission on a workflow action
     * @param roleAPI       loads the workflow special roles
     */
    @VisibleForTesting
    WorkflowAjaxReadAccess(final LayoutAPI layoutAPI, final PermissionAPI permissionAPI,
            final RoleAPI roleAPI) {
        this.layoutAPI = layoutAPI;
        this.permissionAPI = permissionAPI;
        this.roleAPI = roleAPI;
    }

    /**
     * Tells whether the user may read the role list used by the role pickers: a back-end user who
     * can open one of the {@link #ROLE_PICKER_PORTLETS}.
     *
     * @param user the {@link User} making the request
     * @return {@code true} when the user may read the list
     */
    boolean canReadRoles(final User user) {
        return hasAnyPortlet(user, ROLE_PICKER_PORTLETS);
    }

    /**
     * Tells whether the user may read the step list of a workflow scheme: a back-end user who can
     * open one of the {@link #STEP_LIST_PORTLETS}.
     *
     * @param user the {@link User} making the request
     * @return {@code true} when the user may read the list
     */
    boolean canReadSteps(final User user) {
        return hasAnyPortlet(user, STEP_LIST_PORTLETS);
    }

    /**
     * Tells whether the user may list the users and roles a workflow action can be assigned to.
     * Only back-end users qualify, and then when they hold USE on the action, can open the
     * Workflow Schemes portlet, or the action's "who can use" holds one of the "Anyone who can ..."
     * roles. The request carries no content, and those roles are resolved against the content
     * only when the action is fired, so such an action counts as usable here.
     *
     * @param user   the {@link User} making the request
     * @param action the {@link WorkflowAction}, or {@code null} when no action matched the id
     * @return {@code true} when the user may list the assignees
     */
    boolean canListAssignees(final User user, final WorkflowAction action) {
        if (null == action || !isBackendUser(user)) {
            return false;
        }
        try {
            return permissionAPI.doesUserHavePermission(action, PermissionAPI.PERMISSION_USE, user, false)
                    || layoutAPI.doesUserHaveAccessToPortlet(PortletID.WORKFLOW_SCHEMES.toString(), user)
                    || isOpenToContentRoles(action);
        } catch (final DotDataException | DotSecurityException e) {
            Logger.warn(this, String.format("Unable to verify access to workflow action '%s' for user ID '%s': %s",
                    action.getId(), user.getUserId(), e.getMessage()));
            return false;
        }
    }

    /**
     * Tells whether the action's "who can use" holds one of the "Anyone who can ..." workflow roles.
     */
    private boolean isOpenToContentRoles(final WorkflowAction action)
            throws DotDataException, DotSecurityException {
        for (final Role role : roleAPI.findWorkflowSpecialRoles()) {
            if (permissionAPI.doesRoleHavePermission(action, PermissionAPI.PERMISSION_USE, role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether the user can open at least one of the given portlets. A failed lookup counts
     * as no access.
     */
    private boolean hasAnyPortlet(final User user, final Set<String> portletIds) {
        if (!isBackendUser(user)) {
            return false;
        }
        try {
            for (final String portletId : portletIds) {
                if (layoutAPI.doesUserHaveAccessToPortlet(portletId, user)) {
                    return true;
                }
            }
        } catch (final DotDataException e) {
            Logger.warn(this, String.format("Unable to verify portlet access for user ID '%s': %s",
                    user.getUserId(), e.getMessage()));
        }
        return false;
    }

    /**
     * Tells whether the user is present and a back-end user.
     */
    private boolean isBackendUser(final User user) {
        return null != user && user.isBackendUser();
    }
}
