import { DotMenu, DotMenuItem } from '@dotcms/dotcms-models';

/**
 * Pure utility functions for menu navigation logic.
 * These functions provide immutable operations for managing menu state and navigation.
 */

/**
 * Mapping of non-menu section IDs to their corresponding menu section IDs.
 * Used to redirect certain sections to their proper menu equivalents.
 */
export const replaceSectionsMap: Record<string, string> = {
    'edit-page': 'site-browser',
    analytics: 'analytics-dashboard'
};

/**
 * Replaces a non-menu section ID with its corresponding menu section ID.
 *
 * @param id - The section ID to check for replacement
 * @returns The replacement menu section ID if found, undefined otherwise
 */
export const replaceIdForNonMenuSection = (id: string): string | undefined => {
    return replaceSectionsMap[id];
};

/**
 * Properties for setting active menu items based on navigation context.
 */
export interface DotActiveItemsProps {
    /** The current URL path */
    url: string;
    /** Whether the navigation menu is collapsed */
    collapsed: boolean;
    /** Optional menu ID to match against */
    menuId?: string;
    /** The previous URL path for navigation context */
    previousUrl: string;
}

/**
 * Extended properties for setting active menu items that includes the menus array.
 */
export interface DotActiveItemsFromParentProps extends DotActiveItemsProps {
    /** Array of menu objects to process */
    menus: DotMenu[];
}

/**
 * Resets all menus and their items to an inactive state in an immutable way.
 *
 * @param menus - Array of menu objects to reset
 * @returns New array of menus with all active and isOpen flags set to false
 */
export function resetAllMenus(menus: DotMenu[]): DotMenu[] {
    return menus.map((menu) => ({
        ...menu,
        active: false,
        isOpen: false,
        menuItems: menu.menuItems.map((item) => ({
            ...item,
            active: false
        }))
    }));
}

/**
 * Finds and activates a menu item in the menus array in an immutable way.
 *
 * @param menus - Array of menu objects to search
 * @param urlId - The ID extracted from the URL to match against menu items
 * @param menuId - Optional menu ID to match against (if provided, both menu and item must match)
 * @param isOpen - Whether the menu should be marked as open (default: true)
 * @returns New array of menus with the matching item activated, or null if no match is found
 */
export function findAndActivateMenuItem(
    menus: DotMenu[],
    urlId: string,
    menuId?: string,
    isOpen = true
): DotMenu[] | null {
    const isMatchingItem = (item: DotMenuItem, menu: DotMenu) =>
        (menuId && item.id === urlId && menu.id === menuId) || (!menuId && item.id === urlId);

    const menuIndex = menus.findIndex((menu) =>
        menu.menuItems.some((item) => isMatchingItem(item, menu))
    );

    if (menuIndex === -1) {
        return null;
    }

    const menu = menus[menuIndex];
    const itemIndex = menu.menuItems.findIndex((item) => isMatchingItem(item, menu));

    return menus.map((m, menuIdx) => {
        if (menuIdx !== menuIndex) return m;

        return {
            ...m,
            active: true,
            isOpen,
            menuItems: m.menuItems.map((menuItem, itemIdx) =>
                itemIdx === itemIndex ? { ...menuItem, active: true } : menuItem
            )
        };
    });
}

/**
 * Gets the active menu items based on menu ID and URL.
 * This function resets all menus and then activates the matching menu item.
 * Used when navigating through Angular routing.
 *
 * @param props - Configuration object containing menus, menuId, collapsed state, and url
 * @returns Array of menus with the matching item activated, or all menus reset if no match
 */
export function getActiveMenuFromMenuId({
    menus,
    menuId,
    collapsed,
    url
}: DotActiveItemsFromParentProps): DotMenu[] {
    const urlId = getTheUrlId(url);
    const resetMenus = resetAllMenus(menus);
    const updatedMenus = findAndActivateMenuItem(resetMenus, urlId, menuId, !collapsed);
    return updatedMenus || resetMenus;
}

/**
 * Checks if the current URL represents a detail/edit page.
 *
 * @param id - The ID extracted from the URL
 * @param url - The full URL path
 * @returns True if the URL contains 'edit' and includes the ID
 */
export function isDetailPage(id: string, url: string): boolean {
    return url.split('/').includes('edit') && url.includes(id);
}

/**
 * Checks if any menu in the array is currently active.
 *
 * @param menus - Array of menu objects to check
 * @returns True if at least one menu has its active flag set to true
 */
export function isMenuActive(menus: DotMenu[]): boolean {
    return !!menus.find((item) => item.active);
}

/**
 * Checks if the current navigation is from the site browser to an edit page.
 * Used to determine if menu updates should be skipped.
 *
 * @param menuId - The current menu ID
 * @param previousUrl - The previous URL path
 * @returns True if navigating from site-browser to edit-page
 */
export function isEditPageFromSiteBrowser(menuId: string, previousUrl: string): boolean {
    return menuId === 'edit-page' && previousUrl === '/c/site-browser';
}

/**
 * Extracts the relevant ID from a URL path.
 * For legacy URLs (starting with /c/), returns the last segment.
 * For other URLs, returns the first segment.
 *
 * @param url - The URL path to extract the ID from
 * @returns The extracted ID, or empty string if not found
 *
 * @example
 * getTheUrlId('/c/content-types') // returns 'content-types'
 * getTheUrlId('/sites') // returns 'sites'
 */
export function getTheUrlId(url: string): string {
    const urlSegments: string[] = url.split('/').filter(Boolean);

    if (urlSegments[0] === 'c') {
        return urlSegments.pop() || '';
    }

    return urlSegments[0] || '';
}

/**
 * Adds menu links to each menu item based on whether it's angular or legacy.
 * For angular items, uses the url property directly.
 * For legacy items, constructs a URL in the format /c/{itemId}.
 *
 * @param menu - Array of DotMenu objects
 * @returns Array of DotMenu objects with menuLink properties added
 */
export function addMenuLinks(menu: DotMenu[]): DotMenu[] {
    return menu.map((menuGroup: DotMenu) => {
        menuGroup.menuItems.forEach((menuItem: DotMenuItem) => {
            menuItem.menuLink = menuItem.angular ? menuItem.url : `/c/${menuItem.id}`;
        });

        return menuGroup;
    });
}
