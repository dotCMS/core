import { entityConfig } from '@ngrx/signals/entities';

import { MenuItemEntity } from '@dotcms/dotcms-models';

/**
 * Menu slice state interface.
 * Contains the menu entity state, navigation collapsed state, and open parent menu ID.
 */
export interface MenuSlice {
    /**
     * Whether the navigation menu is collapsed.
     */
    isNavigationCollapsed: boolean;

    /**
     * ID of the currently open parent menu group.
     * Only one parent menu group can be open at a time.
     * Set to `null` when no parent menu group is open.
     */
    openParentMenuId: string | null;
}

/**
 * Entity configuration for menu items.
 * Uses a composite key of item ID + parent menu ID as the unique identifier.
 * This allows the same menu item to appear in multiple parent menu groups.
 */
export const menuConfig = entityConfig({
    entity: {} as MenuItemEntity,
    collection: 'menuItems',
    selectId: (item: MenuItemEntity) => `${item.id}__${item.parentMenuId}`
});

export const initialMenuSlice: MenuSlice = {
    isNavigationCollapsed: true,
    openParentMenuId: null
};
