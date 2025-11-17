import { DotMenu, DotMenuItem } from '@dotcms/dotcms-models';

// Pure utility functions for menu navigation logic

export const replaceSectionsMap: Record<string, string> = {
    'edit-page': 'site-browser',
    analytics: 'analytics-dashboard'
};

export const replaceIdForNonMenuSection = (id: string): string | undefined => {
    return replaceSectionsMap[id];
};

export interface DotActiveItemsProps {
    url: string;
    collapsed: boolean;
    menuId?: string;
    previousUrl: string;
}

export interface DotActiveItemsFromParentProps extends DotActiveItemsProps {
    menus: DotMenu[];
}

export function getActiveMenuFromMenuId({
    menus,
    menuId,
    collapsed,
    url
}: DotActiveItemsFromParentProps): DotMenu[] {
    // Extract the ID from the URL for comparison with menu item IDs
    const urlId = getTheUrlId(url);

    const resetMenus = menus.map((menu) => ({
        ...menu,
        active: false,
        isOpen: false,
        menuItems: menu.menuItems.map((item) => ({
            ...item,
            active: false
        }))
    }));

    // Search for matching menu item and activate it
    const isMatchingItem = (item: DotMenuItem, menu: DotMenu) =>
        (menuId && item.id === urlId && menu.id === menuId) || (!menuId && item.id === urlId);

    const menuIndex = resetMenus.findIndex((menu) =>
        menu.menuItems.some((item) => isMatchingItem(item, menu))
    );

    if (menuIndex !== -1) {
        const menu = resetMenus[menuIndex];
        const itemIndex = menu.menuItems.findIndex((item) => isMatchingItem(item, menu));

        // Create new menu object with active item (immutable update)
        return resetMenus.map((m, menuIdx) => {
            if (menuIdx !== menuIndex) return m;

            return {
                ...m,
                active: true,
                isOpen: !collapsed,
                menuItems: m.menuItems.map((menuItem, itemIdx) =>
                    itemIdx === itemIndex ? { ...menuItem, active: true } : menuItem
                )
            };
        });
    }

    // No matching item found, return all inactive
    return resetMenus;
}

export function isDetailPage(id: string, url: string): boolean {
    return url.split('/').includes('edit') && url.includes(id);
}

export function isMenuActive(menus: DotMenu[]): boolean {
    return !!menus.find((item) => item.active);
}

export function isEditPageFromSiteBrowser(menuId: string, previousUrl: string): boolean {
    return menuId === 'edit-page' && previousUrl === '/c/site-browser';
}

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
