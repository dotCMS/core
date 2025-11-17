import { Observable } from 'rxjs';

import { map } from 'rxjs/operators';

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
    return menus.map((menu) => {
        menu.active = false;

        menu.menuItems = menu.menuItems.map((item) => ({
            ...item,
            active: false
        }));

        if (menu.id === menuId) {
            menu.active = true;
            menu.isOpen = !collapsed && menu.active;
            menu.menuItems = menu.menuItems.map((item) => ({
                ...item,
                active: item.id === url
            }));
        }

        return menu;
    });
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

    return urlSegments[0] === 'c' ? urlSegments.pop() || '' : urlSegments[0] || '';
}

export const setActiveItems =
    ({ url, collapsed, menuId, previousUrl }: DotActiveItemsProps) =>
    (source: Observable<DotMenu[]>) => {
        let urlId = getTheUrlId(url);

        return source.pipe(
            map((m: DotMenu[]) => {
                if (!url) {
                    return m; // nothing changes.
                }

                const menus: DotMenu[] = [...m];

                if (
                    (menuId && isEditPageFromSiteBrowser(menuId, previousUrl)) ||
                    (isDetailPage(urlId, url) && isMenuActive(menus))
                ) {
                    return null;
                }

                // When user browse using the navigation (Angular Routing)
                if (menuId && menuId !== 'edit-page' && previousUrl) {
                    return getActiveMenuFromMenuId({
                        menus,
                        menuId,
                        collapsed,
                        url: urlId,
                        previousUrl
                    });
                }

                // When user browse using the browser url bar, direct links or reload page
                const replacedId = replaceIdForNonMenuSection(urlId);
                urlId = replacedId || urlId;

                // Reset Active/IsOpen attributes
                for (let i = 0; i < menus.length; i++) {
                    menus[i].active = false;
                    menus[i].isOpen = false;

                    for (let k = 0; k < menus[i].menuItems.length; k++) {
                        menus[i].menuItems[k].active = false;
                    }
                }

                menuLoop: for (let i = 0; i < menus.length; i++) {
                    for (let k = 0; k < menus[i].menuItems.length; k++) {
                        if (menuId) {
                            if (menus[i].menuItems[k].id === urlId && menus[i].id === menuId) {
                                menus[i].active = true;
                                menus[i].isOpen = true;
                                menus[i].menuItems[k].active = true;
                                break menuLoop;
                            }
                        } else if (menus[i].menuItems[k].id === urlId) {
                            menus[i].active = true;
                            menus[i].isOpen = true;
                            menus[i].menuItems[k].active = true;
                            break menuLoop;
                        }
                    }
                }

                return menus;
            })
        );
    };
