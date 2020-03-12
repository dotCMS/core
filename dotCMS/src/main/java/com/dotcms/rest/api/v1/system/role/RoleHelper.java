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
import com.dotmarketing.util.UtilMethods;

import java.util.ArrayList;
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
                if (!roleAPI.hasLayoutToRole(layout, role)) {

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
                if (roleAPI.hasLayoutToRole(layout, role)) {

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
