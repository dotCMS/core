package com.dotcms.rest.api.v1.system.role;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ConflictException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DuplicateRoleException;
import com.dotmarketing.business.DuplicateRoleKeyException;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.RoleNameException;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper to encapsulate Roles logic
 * @author jsanca
 */
@ApplicationScoped
public class RoleHelper {

    private final RoleAPI roleAPI;

    public RoleHelper() {
        this(APILocator.getRoleAPI());
    }

    @Inject
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

        role.setName(roleForm.getRoleName());
        role.setRoleKey(roleForm.getRoleKey());
        role.setEditUsers(roleForm.isCanEditUsers());
        role.setEditPermissions(roleForm.isCanEditPermissions());
        role.setEditLayouts(roleForm.isCanEditLayouts());
        role.setDescription(roleForm.getDescription());

        final String parentRoleId = roleForm.getParentRoleId();
        if (Objects.nonNull(parentRoleId)) {

            if (parentRoleId.equals(roleId)) {
                throw new BadRequestException("A role cannot be its own parent: " + roleId);
            }

            final Role parentRole = this.roleAPI.loadRoleById(parentRoleId);
            if (null == parentRole || !UtilMethods.isSet(parentRole.getId())) {
                throw new DoesNotExistException("Parent role not found: " + parentRoleId);
            }

            // findRoleHierarchy walks getParent() up to the root, so it returns the proposed
            // parent and all its ancestors — if the edited role is among them, the reparent
            // would create a cycle
            for (final Role ancestor : this.roleAPI.findRoleHierarchy(parentRole)) {
                if (roleId.equals(ancestor.getId())) {
                    throw new BadRequestException(String.format(
                            "Cannot move role '%s' under '%s': the target parent is one of its descendants",
                            roleId, parentRoleId));
                }
            }

            role.setParent(parentRole.getId());
        } else {
            role.setParent(role.getId());
        }

        final String date = DateUtil.getCurrentDate();
        ActivityLogger.logInfo(getClass(), "Modifying Role",
                "Date: " + date + "; User:" + modUser.getUserId() + "; RoleID: " + role.getId());
        AdminLogger.log(getClass(), "Modifying Role",
                "Date: " + date + "; User:" + modUser.getUserId() + "; RoleID: " + role.getId());

        final Role updatedRole;
        try {
            updatedRole = this.roleAPI.save(role);
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
}
