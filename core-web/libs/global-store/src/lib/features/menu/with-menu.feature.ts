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
import { DotMenu } from '@dotcms/dotcms-models';

import { initialMenuSlice, menuConfig, type MenuItemEntity } from './menu.slice';

const DOTCMS_MENU_STATUS = 'dotcms.menu.status';

/**
 * Interface for grouped menu items by parent.
 */
export interface MenuGroup {
    id: string;
    label: string;
    icon: string;
    menuItems: MenuItemEntity[];
    isOpen: boolean;
}

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
        withComputed(({ menuItemsEntityMap, menuItemsEntities, openParentId }) => ({
            /**
             * Computed signal that returns menu items grouped by parent.
             *
             * @returns Array of MenuGroup objects with parent information and child items
             */
            menuGroup: computed((): MenuGroup[] => {
                const items = menuItemsEntities();
                const currentOpenParentId = openParentId();

                // Group items by parentId
                const grouped = items.reduce<Record<string, MenuItemEntity[]>>((acc, item) => {
                    const parentId = item.parentId;
                    acc[parentId] = acc[parentId] || [];
                    acc[parentId].push(item);
                    return acc;
                }, {});

                // Transform grouped object into array of MenuGroup
                return Object.entries(grouped).map(([parentId, menuItems]) => {
                    const firstItem = menuItems[0];
                    return {
                        id: parentId,
                        label: firstItem.parentLabel,
                        icon: firstItem.parentIcon,
                        menuItems: menuItems,
                        isOpen: parentId === currentOpenParentId
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
                        parentId: parent.id,
                        parentLabel: parent.tabName,
                        parentIcon: parent.tabIcon,
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
                    console.log(entities, 'entities');

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
                    patchState(store, updateAllEntities({ active: false }, menuConfig));

                    // Then activate the target item
                    patchState(store, updateEntity({ id, changes: { active: true } }, menuConfig));
                },

                /**
                 * Activates a menu item and opens its parent group.
                 * Ensures only one item is active and one parent is open.
                 *
                 * @param menuItemId - The ID of the menu item to activate
                 * @param parentId - The ID of the parent group to open
                 */
                activateMenuItemWithParent: (menuItemId: string, parentId: string | null) => {
                    // Deactivate all items
                    patchState(store, updateAllEntities({ active: false }, menuConfig));

                    // Activate the target item
                    patchState(
                        store,
                        updateEntity({ id: menuItemId, changes: { active: true } }, menuConfig)
                    );

                    // Set the open parent (this automatically closes other parents via computed)
                    patchState(store, { openParentId: parentId });
                },

                /**
                 * Toggles the open state of a parent menu group.
                 * If the specified parent is already open, it closes.
                 * If another parent is open, it closes and opens the specified one.
                 *
                 * @param parentId - The ID of the parent group to toggle
                 */
                toggleParent: (parentId: string) => {
                    const currentOpenId = store.openParentId();
                    const newOpenId = currentOpenId === parentId ? null : parentId;
                    patchState(store, { openParentId: newOpenId });
                },

                /**
                 * Closes all menu parent groups.
                 */
                closeAllParents: () => {
                    patchState(store, { openParentId: null });
                },

                /**
                 * Toggles the navigation menu collapsed/expanded state.
                 */
                toggleNavigation: () => {
                    const isCollapsed = store.isNavigationCollapsed();
                    patchState(store, {
                        isNavigationCollapsed: !isCollapsed
                    });

                    // When collapsing, close all parent groups
                    if (!isCollapsed) {
                        patchState(store, { openParentId: null });
                    } else {
                        // When expanding, open the parent of the active item if there is one
                        const activeItem = store.activeMenuItem();
                        if (activeItem) {
                            patchState(store, { openParentId: activeItem.parentId });
                        }
                    }
                },

                /**
                 * Collapses the navigation menu.
                 * Closes all parent groups when collapsing.
                 */
                collapseNavigation: () => {
                    patchState(store, {
                        isNavigationCollapsed: true,
                        openParentId: null
                    });
                },

                /**
                 * Expands the navigation menu.
                 * Opens the parent of the active item if there is one.
                 */
                expandNavigation: () => {
                    patchState(store, {
                        isNavigationCollapsed: false
                    });

                    // Open the parent of the active item if there is one
                    const activeItem = store.activeMenuItem();
                    if (activeItem) {
                        patchState(store, { openParentId: activeItem.parentId });
                    }
                }
            };
        }),
        withMethods((store) => ({
            /**
             * Loads menu and sets active item based on current URL.
             * Transforms DotMenu array to entities and activates the item matching the URL.
             *
             * @param menuItems - DotMenu array from the API
             * @param portletId - The current URL to find and activate the matching item
             */
            setActiveMenu: (menuItems: DotMenu[], portletId: string) => {
                // Transform and load menu as entities
                store.loadMenu(menuItems);

                if (!portletId) {
                    return;
                }

                // Find the menu item by portletId in all entities

                const allEntities = store.menuItemsEntities();
                console.log(allEntities, 'all');

                const targetItem = allEntities.find((entity) => entity.id === portletId);

                // If found, activate it with its parent
                if (targetItem) {
                    const collapsed = store.isNavigationCollapsed();

                    // Use the composite key for activation
                    const compositeKey = `${targetItem.id}__${targetItem.parentId}`;

                    store.activateMenuItemWithParent(
                        compositeKey,
                        collapsed ? null : targetItem.parentId
                    );
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
