package com.dotcms.rest.api.v1.system.role;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ConflictException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.DuplicateRoleException;
import com.dotmarketing.business.DuplicateRoleKeyException;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.RoleNameException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper to encapsulate Roles logic
 * @author jsanca
 */
public class RoleHelper {

    private final RoleAPI roleAPI;

    public RoleHelper() {
        this(APILocator.getRoleAPI());
    }

    @VisibleForTesting
    public RoleHelper(final RoleAPI roleAPI) {
        this.roleAPI = roleAPI;
    }

    /**
     * Updates an existing role — name, key, description, can-grant flags, and parent
     * (reparent). Mirrors the legacy DWR {@code RoleAjax#updateRole} behavior: a null
     * {@code parentRoleId} turns the role into a root role (parent == own id).
     *
     * Guards (see #36936):
     * <ul>
     *   <li>missing role or parent → {@link DoesNotExistException} (404)</li>
     *   <li>system or locked role → {@link DotSecurityException} (403) — same condition
     *       {@code RoleAPIImpl.save} enforces, surfaced cleanly</li>
     *   <li>reparent to self or to a descendant (cycle) → {@link BadRequestException} (400);
     *       net-new guard vs legacy, prevents hierarchy corruption</li>
     *   <li>invalid name → {@link BadRequestException} (400); duplicate key/name →
     *       {@link ConflictException} (409)</li>
     * </ul>
     *
     * @param roleId   id of the role to update
     * @param roleForm new field values (same shape POST /v1/roles consumes)
     * @param modUser  authenticated user performing the change (audit logging)
     * @return the updated {@link Role}
     */
    @WrapInTransaction
    public Role updateRole(final String roleId, final RoleForm roleForm, final User modUser)
            throws DotDataException, DotSecurityException {

        final Role role = this.roleAPI.loadRoleById(roleId);
        if (null == role || !UtilMethods.isSet(role.getId())) {
            throw new DoesNotExistException("Role not found: " + roleId);
        }

        if (role.isSystem() || role.isLocked()) {
            throw new DotSecurityException(
                    String.format("Role '%s' (%s) is a system or locked role and cannot be updated",
                            role.getName(), role.getId()));
        }

        // validate the parent BEFORE mutating anything — see the copy note below
        final String parentRoleId = roleForm.getParentRoleId();
        Role parentRole = null;
        if (Objects.nonNull(parentRoleId)) {

            if (parentRoleId.equals(roleId)) {
                throw new BadRequestException("A role cannot be its own parent: " + roleId);
            }

            parentRole = this.roleAPI.loadRoleById(parentRoleId);
            if (null == parentRole || !UtilMethods.isSet(parentRole.getId())) {
                throw new DoesNotExistException("Parent role not found: " + parentRoleId);
            }

            // reject the cycle: the edited role must not be an ancestor of the proposed parent
            if (this.roleAPI.isParentRole(role, parentRole)) {
                throw new BadRequestException(String.format(
                        "Cannot move role '%s' under '%s': the target parent is one of its descendants",
                        roleId, parentRoleId));
            }
        }

        // loadRoleById returns the cache-resident instance — apply the update to a detached
        // copy so a save rejected by RoleAPIImpl (duplicate key/name, invalid name) cannot
        // leave phantom values in the role cache
        final Role roleToSave = new Role();
        try {
            BeanUtils.copyProperties(roleToSave, role);
        } catch (final IllegalAccessException | InvocationTargetException e) {
            throw new DotDataException("Error copying role for update: " + roleId, e);
        }

        roleToSave.setName(roleForm.getRoleName());
        roleToSave.setRoleKey(roleForm.getRoleKey());
        roleToSave.setEditUsers(roleForm.isCanEditUsers());
        roleToSave.setEditPermissions(roleForm.isCanEditPermissions());
        roleToSave.setEditLayouts(roleForm.isCanEditLayouts());
        roleToSave.setDescription(roleForm.getDescription());
        roleToSave.setParent(null != parentRole ? parentRole.getId() : role.getId());

        final String date = DateUtil.getCurrentDate();
        ActivityLogger.logInfo(getClass(), "Modifying Role",
                "Date: " + date + "; User:" + modUser.getUserId() + "; RoleID: " + role.getId());
        AdminLogger.log(getClass(), "Modifying Role",
                "Date: " + date + "; User:" + modUser.getUserId() + "; RoleID: " + role.getId());

        final Role updatedRole;
        try {
            updatedRole = this.roleAPI.save(roleToSave);
        } catch (final DuplicateRoleKeyException e) {
            throw new ConflictException(
                    "A role with key '" + roleForm.getRoleKey() + "' already exists", e);
        } catch (final DuplicateRoleException e) {
            throw new ConflictException(
                    "A role named '" + roleForm.getRoleName() + "' already exists under the same parent", e);
        } catch (final RoleNameException e) {
            throw new BadRequestException("Role name is not valid: " + roleForm.getRoleName());
        }

        ActivityLogger.logInfo(getClass(), "Role Modified",
                "Date: " + date + "; User:" + modUser.getUserId() + "; RoleID: " + role.getId());
        AdminLogger.log(getClass(), "Role Modified",
                "Date: " + date + "; User:" + modUser.getUserId() + "; RoleID: " + role.getId());

        return updatedRole;
    }

    /**
     * Deletes an existing role. The deletion CASCADES — legacy parity with
     * {@code RoleAPIImpl.delete}: the role is removed from every user that has it, all its
     * permissions are stripped, and its layout assignments are detached before the role row
     * is removed. Deletion is blocked only where legacy blocks it (see #36939):
     * <ul>
     *   <li>missing role → {@link DoesNotExistException} (404)</li>
     *   <li>system or locked role → {@link DotSecurityException} (403)</li>
     *   <li>role with children → {@link ConflictException} (409); the legacy DWR endpoint
     *       silently returned {@code false} for this case</li>
     *   <li>role referenced by a workflow action's Assign To → {@link ConflictException} (409),
     *       pre-checked here because the same check inside {@code RoleAPIImpl.delete} is
     *       swallowed by its catch(Exception) and would surface as a generic failure</li>
     * </ul>
     *
     * No transaction wrapper here: the pre-flights are reads and {@code roleAPI.delete} is
     * already transactional.
     *
     * @param roleId  id of the role to delete
     * @param modUser authenticated user performing the deletion (audit logging)
     * @return number of users the role was removed from by the cascade
     */
    public int deleteRole(final String roleId, final User modUser)
            throws DotDataException, DotSecurityException {

        final Role role = this.roleAPI.loadRoleById(roleId);
        if (null == role || !UtilMethods.isSet(role.getId())) {
            throw new DoesNotExistException("Role not found: " + roleId);
        }

        if (role.isSystem() || role.isLocked()) {
            throw new DotSecurityException(
                    String.format("Role '%s' (%s) is a system or locked role and cannot be deleted",
                            role.getName(), role.getId()));
        }

        final List<String> children = role.getRoleChildren();
        if (null != children && !children.isEmpty()) {
            throw new ConflictException(String.format(
                    "Role '%s' (%s) has %d child role(s) and cannot be deleted; delete or reparent its children first",
                    role.getName(), role.getId(), children.size()));
        }

        this.checkNoDependentWorkflowActions(role);

        final int usersAffected = this.roleAPI.findUserIdsForRole(role).size();

        // the role row is gone after the delete, so the audit lines must carry enough to
        // identify it (name/key) and its blast radius without a DB lookup
        final String auditDetail = "Date: " + DateUtil.getCurrentDate()
                + "; User:" + modUser.getUserId() + "; RoleID: " + role.getId()
                + "; Name: " + role.getName() + "; Key: " + role.getRoleKey()
                + "; UsersAffected: " + usersAffected;

        ActivityLogger.logInfo(getClass(), "Deleting Role", auditDetail);
        AdminLogger.log(getClass(), "Deleting Role", auditDetail);

        try {
            this.roleAPI.delete(role);
        } catch (final DotDataException | DotStateException e) {
            ActivityLogger.logInfo(getClass(), "Error Deleting Role", auditDetail);
            AdminLogger.log(getClass(), "Error Deleting Role", auditDetail);
            throw e;
        }

        ActivityLogger.logInfo(getClass(), "Role Deleted", auditDetail);
        AdminLogger.log(getClass(), "Role Deleted", auditDetail);

        return usersAffected;
    }

    /**
     * Pre-flight version of {@code RoleAPIImpl#findDependentWorkflowActions}: any workflow
     * action whose Assign To references the role blocks the deletion with a structured 409
     * naming the offending schemes and actions.
     *
     * @param role the role about to be deleted
     */
    private void checkNoDependentWorkflowActions(final Role role)
            throws DotDataException, DotSecurityException {

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final User systemUser = APILocator.systemUser();
        final StringBuilder schemesAndActions = new StringBuilder();
        for (final WorkflowScheme scheme : workflowAPI.findSchemes(true)) {
            final List<WorkflowAction> actions = workflowAPI.findActions(scheme, systemUser,
                    (WorkflowAction action) -> role.getId().equals(action.getNextAssign()));
            if (!actions.isEmpty()) {
                final String conflictingActions = actions.stream()
                        .map(WorkflowAction::getName)
                        .collect(Collectors.joining(", "));
                schemesAndActions.append(scheme.getName()).append(" [action(s) : ")
                        .append(conflictingActions).append("] ");
            }
        }

        if (schemesAndActions.length() > 0) {
            throw new ConflictException(String.format(
                    "Please remove all references to the '%s' Role from the following Workflow Scheme Actions: %s",
                    role.getName(), schemesAndActions));
        }
    }

    /**
     * Grants a role to a user. Legacy parity with the DWR {@code RoleAjax#addUserToRole} path
     * ({@code RoleAPIImpl.addRoleToUser}), whose two behaviors this endpoint keeps (see #36937):
     * <ul>
     *   <li>the grant is IDEMPOTENT — when the user already holds the role, directly or
     *       inherited through the role hierarchy, the API silently no-ops and this method still
     *       succeeds. In particular, granting a role the user only inherits does NOT create a
     *       direct membership</li>
     *   <li>the only grant gate is the role's {@code editUsers} flag — pre-checked here so the
     *       caller gets a clean 403 instead of the API's {@code DotStateException}. Workflow and
     *       system roles are non-grantable because that flag is false on them. Checked AFTER the
     *       already-holds check (legacy order), so re-grants stay a 200 no-op even on frozen
     *       roles</li>
     * </ul>
     *
     * Other guards: missing role or user → {@link DoesNotExistException} (404).
     *
     * @param roleId  id of the role to grant
     * @param userId  id of the user to grant the role to
     * @param modUser authenticated user performing the grant (audit logging)
     * @return the target user the role was granted to
     */
    @WrapInTransaction
    public User addUserToRole(final String roleId, final String userId, final User modUser)
            throws DotDataException, DotSecurityException {

        final Role role = this.roleAPI.loadRoleById(roleId);
        if (null == role || !UtilMethods.isSet(role.getId())) {
            throw new DoesNotExistException("Role not found: " + roleId);
        }

        final User targetUser;
        try {
            targetUser = APILocator.getUserAPI().loadUserById(userId, modUser, false);
        } catch (final NoSuchUserException e) {
            throw new DoesNotExistException("User not found: " + userId);
        }

        // legacy order (RoleAPIImpl.addRoleToUser): the already-holds check comes FIRST, so a
        // re-grant of a held role (direct or inherited) is a silent no-op even when the role's
        // membership has since been frozen (editUsers=false)
        if (this.roleAPI.doesUserHaveRole(targetUser, role)) {
            return targetUser;
        }

        if (!role.isEditUsers()) {
            throw new DotSecurityException(
                    String.format("Users cannot be granted role '%s' (%s): the role does not allow user grants",
                            role.getName(), role.getId()));
        }

        final String auditDetail = "Date: " + DateUtil.getCurrentDate()
                + "; User:" + modUser.getUserId() + "; RoleID: " + role.getId()
                + "; Name: " + role.getName() + "; TargetUser: " + targetUser.getUserId();

        ActivityLogger.logInfo(getClass(), "Adding Role to User", auditDetail);
        AdminLogger.log(getClass(), "Adding Role to User", auditDetail);

        try {
            this.roleAPI.addRoleToUser(role, targetUser);
        } catch (final DotDataException | DotStateException e) {
            ActivityLogger.logInfo(getClass(), "Error Adding Role to User", auditDetail);
            AdminLogger.log(getClass(), "Error Adding Role to User", auditDetail);
            throw e;
        }

        ActivityLogger.logInfo(getClass(), "Role Added to User", auditDetail);
        AdminLogger.log(getClass(), "Role Added to User", auditDetail);

        return targetUser;
    }

    /**
     * Bulk-removes users from a role with PARTIAL-SUCCESS semantics: once the role resolves,
     * the batch never fails as a whole — every removable direct membership is removed and every
     * non-removable entry is reported in the result's {@code skipped} list (see #36938):
     * <ul>
     *   <li>{@code not_found} — no user matches the id</li>
     *   <li>{@code inherited} — the user is not a DIRECT member (holds the role only through
     *       the hierarchy, or not at all). Legacy silently no-ops here: the raw membership
     *       DELETE matches 0 rows ({@code RoleFactoryImpl#removeRoleFromUser}) and the Dojo
     *       portlet only offered checkboxes on direct rows — same outcome, now reported</li>
     *   <li>{@code error} — unexpected per-user failure, logged server-side</li>
     * </ul>
     *
     * DELIBERATELY NOT {@code @WrapInTransaction}: partial-success semantics require per-user
     * commits — each {@code roleAPI.removeRoleFromUser} call is already transactional, and a
     * batch-level wrapper would roll back users 1..N-1 when user N fails.
     *
     * @param roleId  id of the role to remove users from
     * @param userIds ids of the users to remove
     * @param modUser authenticated user performing the removal (audit logging)
     * @return per-user outcomes: removed user ids plus skipped entries with reasons
     */
    public RoleUsersRemovalView removeUsersFromRole(final String roleId, final Set<String> userIds,
            final User modUser) throws DotDataException, DotSecurityException {

        final Role role = this.roleAPI.loadRoleById(roleId);
        if (null == role || !UtilMethods.isSet(role.getId())) {
            throw new DoesNotExistException("Role not found: " + roleId);
        }

        final List<String> removedUserIds = new ArrayList<>();
        final List<SkippedUserView> skipped = new ArrayList<>();

        for (final String userId : userIds) {
            final String auditDetail = "Date: " + DateUtil.getCurrentDate()
                    + "; User:" + modUser.getUserId() + "; RoleID: " + role.getId()
                    + "; Name: " + role.getName() + "; TargetUser: " + userId;
            try {
                final User targetUser;
                try {
                    targetUser = APILocator.getUserAPI().loadUserById(userId, modUser, false);
                } catch (final NoSuchUserException e) {
                    skipped.add(SkippedUserView.builder().userId(userId)
                            .reason(SkippedUserView.REASON_NOT_FOUND).build());
                    continue;
                }

                // direct-membership check per user, served by the role cache — scales with the
                // batch size, not the role's total membership, and reads at removal time
                final boolean directMember =
                        this.roleAPI.loadRolesForUser(targetUser.getUserId(), false).stream()
                                .anyMatch(userRole -> role.getId().equals(userRole.getId()));
                if (!directMember) {
                    skipped.add(SkippedUserView.builder().userId(userId)
                            .reason(SkippedUserView.REASON_INHERITED).build());
                    continue;
                }

                this.roleAPI.removeRoleFromUser(role, targetUser);

                ActivityLogger.logInfo(getClass(), "Role Removed from User", auditDetail);
                AdminLogger.log(getClass(), "Role Removed from User", auditDetail);
                SecurityLogger.logInfo(getClass(), "Removing role:'" + role.getName()
                        + "' from user:" + targetUser.getUserId()
                        + " email:" + targetUser.getEmailAddress()
                        + " by user:" + modUser.getUserId());

                removedUserIds.add(targetUser.getUserId());
            } catch (final Exception e) {
                Logger.error(this, String.format(
                        "Error removing user '%s' from role '%s': %s", userId, roleId, e.getMessage()), e);
                ActivityLogger.logInfo(getClass(), "Error Removing Role from User", auditDetail);
                AdminLogger.log(getClass(), "Error Removing Role from User", auditDetail);
                skipped.add(SkippedUserView.builder().userId(userId)
                        .reason(SkippedUserView.REASON_ERROR).build());
            }
        }

        return RoleUsersRemovalView.builder()
                .removedUserIds(removedUserIds)
                .skipped(skipped)
                .build();
    }

    /**
     * Saves only the existing layouts on layoutIds, any issue previous added not in the list will be removed
     * @param role
     * @param layoutIds
     * @param layoutAPI
     * @param roleAPI
     * @param systemEventsAPI
     * @throws DotDataException
     */
    @WrapInTransaction
    public List<String> saveRoleLayouts(final Role role, final Set<String> layoutIds,
                                final LayoutAPI layoutAPI, final RoleAPI roleAPI,
                                final SystemEventsAPI systemEventsAPI) throws DotDataException {

        final List<Layout> layouts      = layoutAPI.loadLayoutsForRole(role);
        final List<String> layoutsAdded = new ArrayList<>();
        final Map<String, Layout> currentLayoutMaps = layouts.stream().collect(
                Collectors.toMap(layout -> layout.getId(), layout -> layout));

        //Remove all layouts not included in the layoutIds list
        layoutIds.forEach(layoutId -> currentLayoutMaps.remove(layoutId));
        for(final Map.Entry<String, Layout> layoutToRemoveEntry : currentLayoutMaps.entrySet()) {
            roleAPI.removeLayoutFromRole(layoutToRemoveEntry.getValue(), role);
        }

        // Add new layouts
        for(final String changedLayout : layoutIds) {

            final Layout layout = layoutAPI.findLayout(changedLayout);
            if (null != layout && UtilMethods.isSet(layout.getId())) {
                if (!roleAPI.roleHasLayout(layout, role)) {

                    roleAPI.addLayoutToRole(layout, role);
                    layoutsAdded.add(layout.getId());
                }
            }
        }

        //Send a websocket event to notificate a layout change
        systemEventsAPI.pushAsync(SystemEventType.UPDATE_PORTLET_LAYOUTS, new Payload(layoutsAdded));

        return layoutsAdded;
    }

    /**
     * Saves only the existing layouts on layoutIds, any issue previous added not in the list will be removed
     * @param role
     * @param layoutIds
     * @param layoutAPI
     * @param roleAPI
     * @param systemEventsAPI
     * @throws DotDataException
     */
    @WrapInTransaction
    public List<String> deleteRoleLayouts(final Role role, final Set<String> layoutIds,
                                        final LayoutAPI layoutAPI, final RoleAPI roleAPI,
                                        final SystemEventsAPI systemEventsAPI) throws DotDataException {

        final List<String> layoutsDeleted = new ArrayList<>();
        // Delete layout new layouts
        for(final String toDeleteLayout : layoutIds) {

            final Layout layout = layoutAPI.findLayout(toDeleteLayout);
            if (null != layout && UtilMethods.isSet(layout.getId())) {
                if (roleAPI.roleHasLayout(layout, role)) {

                    roleAPI.removeLayoutFromRole(layout, role);
                    layoutsDeleted.add(layout.getId());
                }
            }
        }

        //Send a websocket event to notificate a layout change
        systemEventsAPI.pushAsync(SystemEventType.DELETE_PORTLET_LAYOUTS, new Payload(layoutsDeleted));

        return layoutsDeleted;
    }

    /**
     * Builds the {@link RoleView}s for the given roles, resolving the {@code userCount} of every
     * view (parents and, when requested, their hydrated children) with a single aggregated query
     * instead of one query per role.
     *
     * @param roles             the roles to build views for, order is preserved
     * @param loadChildrenRoles when true, each role's direct children are hydrated as child views
     * @param roleAPI           the {@link RoleAPI} used to load children and resolve counts
     * @return the views in the same order as the given roles
     * @throws DotDataException if loading a child role or the count query fails
     */
    public List<RoleView> toRoleViews(final List<Role> roles, final boolean loadChildrenRoles,
                                      final RoleAPI roleAPI) throws DotDataException {

        final List<String> allRoleIds = new ArrayList<>();
        final Map<String, List<Role>> childrenByParentId = new LinkedHashMap<>();

        for (final Role role : roles) {

            allRoleIds.add(role.getId());
            if (loadChildrenRoles && null != role.getRoleChildren()) {

                final List<Role> children = new ArrayList<>();
                for (final String childRoleId : role.getRoleChildren()) {

                    final Role child = roleAPI.loadRoleById(childRoleId);
                    if (null == child || !UtilMethods.isSet(child.getId())) {

                        Logger.warn(this, "Child role: " + childRoleId + " of role: "
                                + role.getId() + " does not resolve, skipping it");
                        continue;
                    }
                    children.add(child);
                    allRoleIds.add(childRoleId);
                }
                childrenByParentId.put(role.getId(), children);
            }
        }

        final Map<String, Integer> userCounts = roleAPI.countUsersByRoleIds(allRoleIds);

        final List<RoleView> views = new ArrayList<>();
        for (final Role role : roles) {

            final List<RoleView> childViews = new ArrayList<>();
            for (final Role child : childrenByParentId.getOrDefault(role.getId(), List.of())) {

                childViews.add(new RoleView(child, new ArrayList<>(),
                        userCounts.getOrDefault(child.getId(), 0)));
            }
            views.add(new RoleView(role, childViews,
                    userCounts.getOrDefault(role.getId(), 0)));
        }

        return views;
    }
}
