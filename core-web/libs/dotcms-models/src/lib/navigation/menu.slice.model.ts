import { MenuStatus } from './menu-status.model';

/**
 * Menu slice state interface.
 * Contains the menu entity state, navigation collapsed state, and open parent menu ID.
 */
export interface MenuSlice {
    /**
     * Load status of the menu snapshot. A failed or running load never clears entities that
     * are already on screen.
     */
    status: MenuStatus;

    /**
     * Key of the highlighted menu item (see `getMenuItemKey`), or `null` when none is. It lives
     * outside the entities so replacing the menu does not erase it. After a browser reload it starts
     * `null` and is filled from the URL once the menu has loaded.
     */
    activeKey: string | null;

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
