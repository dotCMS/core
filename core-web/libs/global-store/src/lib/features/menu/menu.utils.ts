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
    // Normalize the URL into an ID to compare with menu item ids
    const urlId = getTheUrlId(url);

    // Reset Active/IsOpen attributes immutably and set active menu/menuItem
    let found = false;
    const updatedMenus = menus.map((menu) => {
        // Reset menu and menuItems
        let menuActive = false;
        let menuIsOpen = false;
        const updatedMenuItems = menu.menuItems.map((item) => {
            let itemActive = false;
            if (!found) {
                if (menuId) {
                    if (item.id === urlId && menu.id === menuId) {
                        menuActive = true;
                        menuIsOpen = !collapsed;
                        itemActive = true;
                        found = true;
                    }
                } else if (item.id === urlId) {
                    menuActive = true;
                    menuIsOpen = !collapsed;
                    itemActive = true;
                    found = true;
                }
            }
            return { ...item, active: itemActive };
        });
        return { ...menu, active: menuActive, isOpen: menuIsOpen, menuItems: updatedMenuItems };
    });
    return updatedMenus;
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
