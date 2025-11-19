import {
    patchState,
    signalStoreFeature,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import {
    addEntities,
    removeAllEntities,
    updateAllEntities,
    updateEntity,
    withEntities
} from '@ngrx/signals/entities';

import { computed, effect, inject } from '@angular/core';

import { DotLocalstorageService } from '@dotcms/data-access';
import { DotMenu, MenuGroup, MenuItemEntity } from '@dotcms/dotcms-models';

import { initialMenuSlice, menuConfig } from './menu.slice';

const DOTCMS_MENU_STATUS = 'dotcms.menu.status';

/**
 * Custom Store Feature for managing menu state using Entity Management.
 *
 * This feature provides state management for menu-related data using NgRx Signal Store
 * entity management capabilities. It offers better performance and cleaner code compared
 * to array-based state management.
 *
 * ## Features
 * - Manages menu items as entities with HashMap for O(1) lookups
 * - Single active item constraint (only one item can be active at a time)
 * - Single open parent constraint (only one parent menu can be open at a time)
 * - Manages navigation collapsed/expanded state
 * - Provides computed selectors for grouped menus and active items
 * - Persists navigation state to localStorage
 * - Full TypeScript support with strict typing
 *
 * ## Constraints
 * - Only one menu item can be active at any given time
 * - Only one parent menu group can be open at any given time
 * - Activating a new item automatically deactivates the previously active item
 * - Opening a parent group automatically closes any other open parent group
 */
export function withMenu() {
    return signalStoreFeature(
        withState(initialMenuSlice),
        withEntities(menuConfig),
        withComputed(({ menuItemsEntityMap, menuItemsEntities, openParentMenuId }) => ({
            /**
             * Computed signal that returns menu items grouped by parent.
             *
             * @returns Array of MenuGroup objects with parent information and child items
             */
            menuGroup: computed((): MenuGroup[] => {
                const items = menuItemsEntities();
                const currentOpenParentMenuId = openParentMenuId();

                // Group items by parentMenuId
                const grouped = items.reduce<Record<string, MenuItemEntity[]>>((acc, item) => {
                    const parentMenuId = item.parentMenuId;
                    acc[parentMenuId] = acc[parentMenuId] || [];
                    acc[parentMenuId].push(item);
                    return acc;
                }, {});

                // Transform grouped object into array of MenuGroup
                return Object.entries(grouped).map(([parentMenuId, menuItems]) => {
                    const firstItem = menuItems[0];
                    return {
                        id: parentMenuId,
                        label: firstItem.parentMenuLabel,
                        icon: firstItem.parentMenuIcon,
                        menuItems: menuItems,
                        isOpen: parentMenuId === currentOpenParentMenuId
                    };
                });
            }),

            /**
             * Computed signal that returns the currently active menu item.
             *
             * @returns The active MenuItemEntity or null if no item is active
             */
            activeMenuItem: computed((): MenuItemEntity | null => {
                const items = menuItemsEntities();
                return items.find((item) => item.active) || null;
            }),

            /**
             * Computed signal that returns the entity map for direct lookups.
             *
             * @returns Record of menu items keyed by ID
             */
            entityMap: computed(() => menuItemsEntityMap()),

            /**
             * Computed signal that returns flattened menu items.
             * Compatible with existing code that expects flat arrays.
             *
             * @returns Array of all MenuItemEntity objects
             */
            flattenMenuItems: computed(() => menuItemsEntities())
        })),
        withMethods((store) => {
            /**
             * Transforms DotMenu array into MenuItemEntity array.
             * Flattens the hierarchical menu structure and adds menuLink property.
             */
            const transformMenuToEntities = (menu: DotMenu[]): MenuItemEntity[] => {
                return menu.flatMap((parent) =>
                    parent.menuItems.map((item) => ({
                        ...item,
                        parentMenuId: parent.id,
                        parentMenuLabel: parent.tabName,
                        parentMenuIcon: parent.tabIcon,
                        menuLink: item.angular ? item.url : `/c/${item.id}`
                    }))
                );
            };

            return {
                /**
                 * Loads menu items from DotMenu array.
                 * Transforms the menu structure and sets all entities.
                 * Clears and re-adds all entities to ensure reactivity works correctly.
                 *
                 * @param menu - Array of DotMenu objects
                 */
                loadMenu: (menu: DotMenu[]) => {
                    const entities = transformMenuToEntities(menu);

                    // Clear all entities first, then add new ones
                    // This ensures all property changes are detected by signals
                    patchState(store, removeAllEntities(menuConfig));
                    patchState(store, addEntities(entities, menuConfig));
                },

                /**
                 * Activates a menu item by its ID.
                 * Automatically deactivates any previously active item.
                 * Ensures only one item is active at a time.
                 *
                 * @param id - The ID of the menu item to activate
                 */
                activateMenuItem: (id: string) => {
                    // First, deactivate all items
                    patchState(
                        store,
                        updateAllEntities({ active: false }, menuConfig),
                        updateEntity({ id, changes: { active: true } }, menuConfig)
                    );
                },

                /**
                 * Activates a menu item and opens its parent menu group.
                 * Ensures only one item is active and one parent menu group is open.
                 *
                 * @param menuItemId - The ID of the menu item to activate
                 * @param parentMenuId - The ID of the parent menu group to open
                 */
                activateMenuItemWithParent: (menuItemId: string, parentMenuId: string | null) => {
                    patchState(
                        store,
                        updateAllEntities({ active: false }, menuConfig),
                        updateEntity({ id: menuItemId, changes: { active: true } }, menuConfig)
                    );

                    // Set the open parent menu group (this automatically closes other parent menu groups via computed)
                    patchState(store, { openParentMenuId: parentMenuId });
                },

                /**
                 * Toggles the open state of a parent menu group.
                 * If the specified parent menu group is already open, it closes.
                 * If another parent menu group is open, it closes and opens the specified one.
                 *
                 * @param parentMenuId - The ID of the parent menu group to toggle
                 */
                toggleParent: (parentMenuId: string) => {
                    const currentOpenId = store.openParentMenuId();
                    const newOpenId = currentOpenId === parentMenuId ? null : parentMenuId;
                    patchState(store, { openParentMenuId: newOpenId });
                },

                /**
                 * Closes all parent menu groups.
                 */
                closeAllParents: () => {
                    patchState(store, { openParentMenuId: null });
                },

                /**
                 * Toggles the navigation menu collapsed/expanded state.
                 */
                toggleNavigation: () => {
                    const isCollapsed = store.isNavigationCollapsed();
                    patchState(store, {
                        isNavigationCollapsed: !isCollapsed
                    });

                    // When collapsing, close all parent menu groups
                    if (!isCollapsed) {
                        patchState(store, { openParentMenuId: null });
                    } else {
                        // When expanding, open the parent menu group of the active item if there is one
                        const activeItem = store.activeMenuItem();
                        if (activeItem) {
                            patchState(store, { openParentMenuId: activeItem.parentMenuId });
                        }
                    }
                },

                /**
                 * Collapses the navigation menu.
                 * Closes all parent menu groups when collapsing.
                 */
                collapseNavigation: () => {
                    patchState(store, {
                        isNavigationCollapsed: true,
                        openParentMenuId: null
                    });
                },

                /**
                 * Expands the navigation menu.
                 * Opens the parent menu group of the active item if there is one.
                 */
                expandNavigation: () => {
                    patchState(store, {
                        isNavigationCollapsed: false
                    });

                    // Open the parent menu group of the active item if there is one
                    const activeItem = store.activeMenuItem();
                    if (activeItem) {
                        patchState(store, { openParentMenuId: activeItem.parentMenuId });
                    }
                }
            };
        }),
        withMethods((store) => ({
            /**
             * Loads menu and sets active item based on current URL.
             * Transforms DotMenu array to entities and activates the item matching the URL.
             *
             * @param portletId - The ID of the menu item (portlet) to activate
             * @param parentMenuId - The ID of the parent menu group
             * @param menuItems - Optional DotMenu array from the API to load
             */
            setActiveMenu: (portletId: string, parentMenuId: string, menuItems?: DotMenu[]) => {
                // menuItems is used to handle the event service UPDATE_PORTLET_LAYOUTS,
                // this is used to load the menu items from the API
                if (menuItems) {
                    store.loadMenu(menuItems);
                }

                if (!portletId) {
                    return;
                }

                // If found, activate it with its parent menu group
                if (portletId && parentMenuId) {
                    const collapsed = store.isNavigationCollapsed();

                    const compositeKey = `${portletId}__${parentMenuId}`;
                    store.activateMenuItemWithParent(compositeKey, collapsed ? null : parentMenuId);
                }
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
