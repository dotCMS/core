import {
    patchState,
    signalStoreFeature,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed, effect, inject } from '@angular/core';

import { DotLocalstorageService } from '@dotcms/data-access';
import { DotMenu, DotMenuItem } from '@dotcms/dotcms-models';

import { initialMenuSlice } from './menu.slice';
import {
    getActiveMenuFromMenuId,
    getTheUrlId,
    isDetailPage,
    isEditPageFromSiteBrowser,
    isMenuActive,
    replaceIdForNonMenuSection,
    type DotActiveItemsProps
} from './menu.utils';

const DOTCMS_MENU_STATUS = 'dotcms.menu.status';

/**
 * Custom Store Feature for managing menu state using DotMenu interface.
 *
 * This feature provides state management for menu-related data including
 * menu items, navigation collapsed state, active items, and navigation operations.
 *
 * ## Features
 * - Manages menu items using DotMenu interface
 * - Tracks active menu item by ID
 * - Manages navigation collapsed/expanded state
 * - Provides methods for menu state manipulation and navigation
 * - Includes computed selectors for common use cases
 * - Persists navigation state to localStorage
 * - Full TypeScript support with strict typing
 *
 */
export function withMenu() {
    return signalStoreFeature(
        withState(initialMenuSlice),
        withComputed(({ menuItems, activeMenuItemId }) => ({
            /**
             * Computed signal that finds and returns the active menu item.
             *
             * @returns The active DotMenu or null if not found
             */
            activeMenuItem: computed(() => {
                const activeId = activeMenuItemId();
                if (!activeId) return null;

                return menuItems().find((menu) => menu.id === activeId) || null;
            }),

            /**
             * Computed signal that checks if a menu item is active.
             *
             * @returns A function that takes a menu item ID and returns whether it's active
             */
            isMenuItemActive: computed(() => (id: string) => activeMenuItemId() === id),

            /**
             * Computed signal that finds a menu by ID.
             *
             * @returns A function that takes an ID and returns the DotMenu or null
             */
            findMenuItemById: computed(() => (id: string): DotMenu | null => {
                return menuItems().find((menu) => menu.id === id) || null;
            }),

            /**
             * Computed signal that returns the flattened menu items for breadcrumbs.
             *
             * @returns Array of DotMenuItem objects with labelParent property
             */
            flattenMenuItems: computed(() => {
                const menu = menuItems();
                return menu.reduce<DotMenuItem[]>((acc, menu: DotMenu) => {
                    const items = menu.menuItems.map((item) => ({
                        ...item,
                        labelParent: menu.tabName
                    }));
                    return [...acc, ...items];
                }, []);
            })
        })),
        withMethods((store) => ({
            /**
             * Sets the menu items array.
             *
             * @param menuItems - Array of DotMenu objects
             */
            setMenuItems: (menuItems: DotMenu[]) => {
                patchState(store, { menuItems });
            },

            /**
             * Sets the active menu item ID.
             *
             * @param id - The ID of the menu item to set as active
             */
            setActiveMenuItemId: (id: string | null) => {
                patchState(store, { activeMenuItemId: id });
            },

            /**
             * Sets a menu as open by its ID.
             *
             * @param id - The menu ID to set as open
             */
            setMenuOpen: (id: string) => {
                const updatedMenu: DotMenu[] = store.menuItems().map((menu: DotMenu) => {
                    menu.isOpen = menu.id === id ? !menu.isOpen : false;
                    return menu;
                });
                patchState(store, {
                    menuItems: updatedMenu
                });
            },

            /**
             * Closes all menu sections.
             */
            closeAllMenuSections: () => {
                const closedMenu: DotMenu[] = store.menuItems().map((menu: DotMenu) => {
                    menu.isOpen = false;
                    return menu;
                });
                patchState(store, {
                    menuItems: closedMenu
                });
            },

            /**
             * Toggles the navigation menu collapsed/expanded state.
             */
            toggleNavigation: () => {
                const isCollapsed = store.isNavigationCollapsed();
                patchState(store, {
                    isNavigationCollapsed: !isCollapsed
                });
                if (!isCollapsed) {
                    // Close all sections when collapsing
                    const closedMenu: DotMenu[] = store.menuItems().map((menu: DotMenu) => ({
                        ...menu,
                        isOpen: false
                    }));
                    patchState(store, {
                        menuItems: closedMenu
                    });
                } else {
                    // Open active sections when expanding
                    const expandedMenu: DotMenu[] = store.menuItems().map((menu: DotMenu) => {
                        let isActive = false;
                        menu.menuItems.forEach((item: DotMenuItem) => {
                            if (item.active) {
                                isActive = true;
                            }
                        });
                        menu.isOpen = isActive;
                        return menu;
                    });
                    patchState(store, {
                        menuItems: expandedMenu
                    });
                }
            },

            /**
             * Collapses the navigation menu.
             */
            collapseNavigation: () => {
                patchState(store, {
                    isNavigationCollapsed: true
                });
                const closedMenu: DotMenu[] = store.menuItems().map((menu: DotMenu) => ({
                    ...menu,
                    isOpen: false
                }));
                patchState(store, {
                    menuItems: closedMenu
                });
            },

            /**
             * Expands the navigation menu.
             */
            expandNavigation: () => {
                patchState(store, {
                    isNavigationCollapsed: false
                });
                const expandedMenu: DotMenu[] = store.menuItems().map((menu: DotMenu) => {
                    let isActive = false;
                    menu.menuItems.forEach((item: DotMenuItem) => {
                        if (item.active) {
                            isActive = true;
                        }
                    });
                    return { ...menu, isOpen: isActive };
                });

                patchState(store, {
                    menuItems: expandedMenu
                });
            },

            /**
             * Resets the menu state to initial values.
             */
            resetMenuState: () => {
                patchState(store, initialMenuSlice);
            },

            /**
             * Sets active menu items based on navigation context.
             * This method handles the logic for determining which menu items should be active
             * based on the current URL, menu ID, and navigation state.
             *
             * @param props - Configuration object containing URL, collapsed state, menuId, and previousUrl
             * @returns The updated menu items array, or null if the menu should not be updated
             */
            setActiveMenuItems: (props: DotActiveItemsProps): DotMenu[] | null => {
                const { url, collapsed, menuId, previousUrl } = props;
                const currentMenus = store.menuItems();

                if (!url) {
                    return currentMenus; // nothing changes
                }

                const menus: DotMenu[] = [...currentMenus];
                let urlId = getTheUrlId(url);

                // Check if we should skip updating the menu
                if (
                    (menuId && isEditPageFromSiteBrowser(menuId, previousUrl)) ||
                    (isDetailPage(urlId, url) && isMenuActive(menus))
                ) {
                    return null;
                }

                // When user browse using the navigation (Angular Routing)
                if (menuId && menuId !== 'edit-page' && previousUrl) {
                    const updatedMenus = getActiveMenuFromMenuId({
                        menus,
                        menuId,
                        collapsed: collapsed ?? store.isNavigationCollapsed(),
                        url: urlId,
                        previousUrl
                    });
                    patchState(store, { menuItems: updatedMenus });
                    return updatedMenus;
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

                // Find and activate the matching menu item
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

                patchState(store, { menuItems: menus });
                return menus;
            }
        })),
        withHooks({
            onInit(store) {
                // Load navigation collapsed state from localStorage
                const dotLocalstorageService = inject(DotLocalstorageService);
                const savedMenuStatus = dotLocalstorageService.getItem<boolean>(DOTCMS_MENU_STATUS);
                if (savedMenuStatus !== null) {
                    patchState(store, {
                        isNavigationCollapsed: savedMenuStatus === false ? false : true
                    });
                }

                // Listen to localStorage changes for menu status
                dotLocalstorageService
                    .listen<boolean>(DOTCMS_MENU_STATUS)
                    .subscribe((collapsed: boolean) => {
                        if (collapsed) {
                            store.collapseNavigation();
                        } else {
                            store.expandNavigation();
                        }
                    });

                // Persist navigation collapsed state to localStorage whenever it changes
                effect(() => {
                    const isCollapsed = store.isNavigationCollapsed();
                    dotLocalstorageService.setItem<boolean>(DOTCMS_MENU_STATUS, isCollapsed);
                });
            }
        })
    );
}
