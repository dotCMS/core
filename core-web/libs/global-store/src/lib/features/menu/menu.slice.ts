import { entityConfig } from '@ngrx/signals/entities';

import { DotMenuItem } from '@dotcms/dotcms-models';

/**
 * Extended menu item entity with parent information.
 * This interface extends DotMenuItem to include parent relationships
 * needed for entity management with NgRx Signals.
 */
export interface MenuItemEntity extends DotMenuItem {
    /**
     * ID of the parent menu group this item belongs to.
     */
    parentId: string;

    /**
     * Label of the parent menu group.
     */
    parentLabel: string;

    /**
     * Icon of the parent menu group.
     */
    parentIcon: string;
}

/**
 * Menu slice state interface.
 * Contains the menu entity state, navigation collapsed state, and open parent ID.
 */
export interface MenuSlice {
    /**
     * Whether the navigation menu is collapsed.
     */
    isNavigationCollapsed: boolean;

    /**
     * ID of the currently open parent menu group.
     * Only one parent can be open at a time.
     * Set to `null` when no menu group is open.
     */
    openParentId: string | null;
}

/**
 * Entity configuration for menu items.
 * Uses a composite key of item ID + parent ID as the unique identifier.
 * This allows the same menu item to appear in multiple parent groups.
 */
export const menuConfig = entityConfig({
    entity: {} as MenuItemEntity,
    collection: 'menuItems',
    selectId: (item: MenuItemEntity) => `${item.id}__${item.parentId}`
});

export const initialMenuSlice: MenuSlice = {
    isNavigationCollapsed: true,
    openParentId: null
};
