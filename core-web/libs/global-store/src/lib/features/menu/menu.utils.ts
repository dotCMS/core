import { DotMenu } from '@dotcms/dotcms-models';

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
    for (let i = 0; i < resetMenus.length; i++) {
        for (let k = 0; k < resetMenus[i].menuItems.length; k++) {
            const item = resetMenus[i].menuItems[k];
            const menu = resetMenus[i];

            // Determine if this item should be activated
            const isMatchingItem =
                (menuId && item.id === urlId && menu.id === menuId) ||
                (!menuId && item.id === urlId);

            if (isMatchingItem) {
                // Create new menu object with active item (immutable update)
                return resetMenus.map((m, menuIdx) => {
                    if (menuIdx !== i) return m;

                    return {
                        ...m,
                        active: true,
                        isOpen: !collapsed,
                        menuItems: m.menuItems.map((menuItem, itemIdx) =>
                            itemIdx === k ? { ...menuItem, active: true } : menuItem
                        )
                    };
                });
            }
        }
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
