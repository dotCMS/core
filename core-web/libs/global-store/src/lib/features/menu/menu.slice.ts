import { DotMenu } from '@dotcms/dotcms-models';

/**
 * Menu slice state interface.
 * Contains the menu items, navigation collapsed state, and active menu item ID.
 *
 * @property menuItems - Array of DotMenu objects
 * @property isNavigationCollapsed - Whether the navigation menu is collapsed
 * @property activeMenuItemId - ID of the currently active menu item, or null if none
 */
export interface MenuSlice {
    /**
     * Array of DotMenu objects representing the navigation menu structure.
     */
    menuItems: DotMenu[];

    /**
     * Whether the navigation menu is collapsed.
     */
    isNavigationCollapsed: boolean;

    /**
     * ID of the currently active menu item.
     * Set to `null` when no menu item is active.
     */
    activeMenuItemId: string | null;
}

export const initialMenuSlice: MenuSlice = {
    menuItems: [],
    isNavigationCollapsed: true,
    activeMenuItemId: null
};
