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

import { initialMenuSlice, menuConfig, REPLACE_SECTIONS_MAP } from './menu.slice';

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
        withComputed(({ menuItemsEntityMap, menuItemsEntities, openParentMenuId }) => {
            /**
             * Computed signal that returns menu items grouped by parent.
             *
             * @returns Array of MenuGroup objects with parent information and child items
             */
            const menuGroup = computed((): MenuGroup[] => {
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
                const groups = Object.entries(grouped).map(([parentMenuId, menuItems]) => {
                    const firstItem = menuItems[0];
                    return {
                        id: parentMenuId,
                        label: firstItem.parentMenuLabel,
                        icon: firstItem.parentMenuIcon,
                        menuItems: menuItems,
                        isOpen: parentMenuId === currentOpenParentMenuId
                    };
                });
                return groups;
            });

            /**
             * Computed signal that returns the currently active menu item.
             *
             * @returns The active MenuItemEntity or null if no item is active
             */
            const activeMenuItem = computed((): MenuItemEntity | null => {
                const items = menuItemsEntities();
                return items.find((item) => item.active) || null;
            });

            /**
             * Computed signal that returns the entity map for direct lookups.
             *
             * @returns Record of menu items keyed by ID
             */
            const entityMap = computed(() => menuItemsEntityMap());

            /**
             * Computed signal that returns entity keys for debugging.
             * Shows the ID (key) of each menu item entity.
             *
             * @returns Array of entity keys (IDs)
             */
            const entityKeys = computed(() => menuItemsEntities().map((item) => item.id));

            /**
             * Computed signal that returns the first menu item from the first menu group.
             * Used for initial navigation when loading the application.
             *
             * @returns The first MenuItemEntity or null if no items exist
             */
            const firstMenuItem = computed(() => {
                const groups = menuGroup();
                if (groups.length === 0) return null;

                const firstGroup = groups[0];
                return firstGroup.menuItems.length > 0 ? firstGroup.menuItems[0] : null;
            });

            const isGroupActive = computed(() => {
                return menuGroup().some((group) => group.menuItems.some((item) => item.active));
            });

            return {
                menuGroup,
                activeMenuItem,
                entityMap,
                entityKeys,
                firstMenuItem,
                isGroupActive
            };
        }),

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

            /**
             * Loads menu items from DotMenu array.
             * Transforms the menu structure and sets all entities.
             * Clears and re-adds all entities to ensure reactivity works correctly.
             *
             * @param menu - Array of DotMenu objects
             */
            const loadMenu = (menu: DotMenu[]) => {
                const entities = transformMenuToEntities(menu);

                // Clear all entities first, then add new ones
                // This ensures all property changes are detected by signals
                patchState(store, removeAllEntities(menuConfig), addEntities(entities, menuConfig));
            };

            /**
             * Activates a menu item by its ID.
             * Automatically deactivates any previously active item.
             * Ensures only one item is active at a time.
             *
             * @param id - The ID of the menu item to activate
             */
            const activateMenuItem = (id: string) => {
                // First, deactivate all items
                patchState(
                    store,
                    updateAllEntities({ active: false }, menuConfig),
                    updateEntity({ id, changes: { active: true } }, menuConfig)
                );
            };

            /**
             * Activates a menu item and opens its parent menu group.
             * Ensures only one item is active and one parent menu group is open.
             *
             * @param menuItemId - The ID of the menu item to activate
             * @param parentMenuId - The ID of the parent menu group to open
             */
            const activateMenuItemWithParent = (
                menuItemId: string,
                parentMenuId: string | null
            ) => {
                // Check if the menu item exists before attempting to activate it
                const entityMap = store.entityMap();
                if (!entityMap[menuItemId]) {
                    return;
                }

                patchState(
                    store,
                    updateAllEntities({ active: false }, menuConfig),
                    updateEntity({ id: menuItemId, changes: { active: true } }, menuConfig),
                    { openParentMenuId: parentMenuId }
                );
            };

            /**
             * Toggles the open state of a parent menu group.
             * If the specified parent menu group is already open, it closes.
             * If another parent menu group is open, it closes and opens the specified one.
             *
             * @param parentMenuId - The ID of the parent menu group to toggle
             */
            const toggleParent = (parentMenuId: string) => {
                const currentOpenId = store.openParentMenuId();
                const newOpenId = currentOpenId === parentMenuId ? null : parentMenuId;
                patchState(store, { openParentMenuId: newOpenId });
            };

            /**
             * Closes all parent menu groups.
             */
            const closeAllParents = () => {
                patchState(store, { openParentMenuId: null });
            };

            /**
             * Toggles the navigation menu collapsed/expanded state.
             */
            const toggleNavigation = () => {
                patchState(store, (state) => ({
                    isNavigationCollapsed: !state.isNavigationCollapsed
                }));
            };

            /**
             * Collapses the navigation menu.
             * Closes all parent menu groups when collapsing.
             */
            const collapseNavigation = () => {
                patchState(store, {
                    isNavigationCollapsed: true
                });
            };

            /**
             * Expands the navigation menu.
             * Opens the parent menu group of the active item if there is one.
             */
            const expandNavigation = () => {
                patchState(store, {
                    isNavigationCollapsed: false
                });

                // Open the parent menu group of the active item if there is one
                const activeItem = store.activeMenuItem();
                if (activeItem) {
                    patchState(store, { openParentMenuId: activeItem.parentMenuId });
                }
            };
            /**
             * Loads menu and sets active item based on current URL.
             * Uses entity map to find the matching menu item without iterating keys.
             *
             * @param portletId - The ID of the menu item (portlet) to activate
             * @param shortParentMenuId - The first 4 characters of the parent menu ID
             */
            const setActiveMenu = (
                portletId: string,
                shortParentMenuId: string,
                bookmark?: boolean
            ) => {
                if (!portletId) {
                    return;
                }

                // Check if portletId should be replaced according to REPLACE_SECTIONS_MAP
                const resolvedPortletId = REPLACE_SECTIONS_MAP[portletId] || portletId;

                // Direct lookup using the composite key
                const entityMap = store.entityMap();
                let compositeKey = `${resolvedPortletId}__${shortParentMenuId}`;
                const item = entityMap[compositeKey];

                // Fallback for missing shortParentMenuId cases like old bookmarks
                if (bookmark || !shortParentMenuId) {
                    const item = Object.values(entityMap).find((item) => {
                        return item.id === resolvedPortletId || item.id === portletId;
                    });
                    if (item) {
                        compositeKey = `${item.id}__${item.parentMenuId?.substring(0, 4)}`;
                        activateMenuItemWithParent(compositeKey, item.parentMenuId);
                    }
                }

                if (item) {
                    activateMenuItemWithParent(compositeKey, item.parentMenuId);
                }
            };

            /**
             * Gets the page title based on the current URL.
             * Searches through menu items to find a matching menuLink.
             *
             * @param url - The current URL to match against menu items
             * @returns The label of the matching menu item, or empty string if not found
             */
            const getPageTitleByUrl = (url: string): string => {
                const menuItems = store.menuItemsEntities();

                // Find the menu item whose menuLink is contained in the URL
                const matchingItem = menuItems.find((item) => url.indexOf(item.menuLink) >= 0);

                return matchingItem?.label || '';
            };
            return {
                loadMenu,
                activateMenuItem,
                activateMenuItemWithParent,
                toggleParent,
                closeAllParents,
                toggleNavigation,
                collapseNavigation,
                expandNavigation,
                setActiveMenu,
                getPageTitleByUrl
            };
        }),

        withHooks({
            onInit(store) {
                // Load navigation collapsed state from localStorage
                const dotLocalstorageService = inject(DotLocalstorageService);

                const savedMenuStatus = dotLocalstorageService.getItem<boolean>(DOTCMS_MENU_STATUS);
                if (savedMenuStatus !== null) {
                    patchState(store, {
                        isNavigationCollapsed: savedMenuStatus !== false
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
