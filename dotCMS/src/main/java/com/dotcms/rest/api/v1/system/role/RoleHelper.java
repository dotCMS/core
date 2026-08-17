package com.dotcms.rest.api.v1.system.role;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper to encapsulate Roles logic
 * @author jsanca
 */
public class RoleHelper {

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
