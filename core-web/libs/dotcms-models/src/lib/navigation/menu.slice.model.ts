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
