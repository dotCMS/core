import {
    patchState,
    signalStoreFeature,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed } from '@angular/core';

import { MenuItem } from 'primeng/api';

import { initialMenuSlice } from './menu.slice';

/**
 * Custom Store Feature for managing menu state using PrimeNG MenuItem interface.
 *
 * This feature provides state management for menu-related data including
 * menu items, expanded states, active items, and visibility. It works
 * seamlessly with PrimeNG's MenuItem structure.
 *
 * ## Features
 * - Manages menu items using PrimeNG's MenuItem interface
 * - Tracks active menu item by ID
 * - Manages expanded/collapsed submenus
 * - Provides methods for menu state manipulation
 * - Includes computed selectors for common use cases
 * - Full TypeScript support with strict typing
 *
 */
export function withMenu() {
    return signalStoreFeature(
        withState(initialMenuSlice),
        withComputed(({ menuItems, activeMenuItemId }) => ({
            /**
             * Computed signal that finds and returns the active menu item.
             * Searches recursively through all menu items.
             *
             * @returns The active MenuItem or null if not found
             * ```
             */
            activeMenuItem: computed(() => {
                const activeId = activeMenuItemId();
                if (!activeId) return null;

                const findItem = (items: MenuItem[]): MenuItem | null => {
                    for (const item of items) {
                        if (item.id === activeId) return item;
                        if (item.items) {
                            const found = findItem(item.items);
                            if (found) return found;
                        }
                    }
                    return null;
                };

                return findItem(menuItems());
            }),

            /**
             * Computed signal that checks if a menu item is active.
             *
             * @returns A function that takes a menu item ID and returns whether it's active
             */
            isMenuItemActive: computed(() => (id: string) => activeMenuItemId() === id),

            /**
             * Computed signal that finds a menu item by ID.
             *
             * @returns A function that takes an ID and returns the MenuItem or null
             */
            findMenuItemById: computed(() => (id: string): MenuItem | null => {
                const findItem = (items: MenuItem[]): MenuItem | null => {
                    for (const item of items) {
                        if (item.id === id) return item;
                        if (item.items) {
                            const found = findItem(item.items);
                            if (found) return found;
                        }
                    }
                    return null;
                };

                return findItem(menuItems());
            }),

            /**
             * Computed signal that checks if a menu item is expanded.
             *
             * @returns A function that takes a menu item ID and returns whether it's expanded
             */
            isMenuItemExpanded: computed(() => (id: string): boolean => {
                const findItem = (items: MenuItem[]): boolean => {
                    for (const item of items) {
                        if (item.id === id) return item.expanded ?? false;
                        if (item.items) {
                            const result = findItem(item.items);
                            if (item.id === id || result) return result;
                        }
                    }
                    return false;
                };

                return findItem(menuItems());
            }),

            /**
             * Computed signal that returns all visible menu items (recursively).
             *
             * @returns Array of visible MenuItem objects
             */
            visibleMenuItems: computed(() => {
                const filterVisible = (items: MenuItem[]): MenuItem[] => {
                    return items
                        .filter((item) => item.visible !== false)
                        .map((item) => ({
                            ...item,
                            items: item.items ? filterVisible(item.items) : undefined
                        }));
                };

                return filterVisible(menuItems());
            }),

            /**
             * Computed signal that returns the count of expanded menu items.
             *
             * @returns The number of expanded menu items
             */
            expandedMenuItemsCount: computed(() => {
                let count = 0;
                const countExpanded = (items: MenuItem[]): void => {
                    items.forEach((item) => {
                        if (item.expanded) count++;
                        if (item.items) countExpanded(item.items);
                    });
                };

                countExpanded(menuItems());
                return count;
            }),

            /**
             * Computed signal that returns whether any menu items are expanded.
             *
             * @returns `true` if any menu items are expanded, `false` otherwise
             */
            hasExpandedMenuItems: computed(() => {
                const hasExpanded = (items: MenuItem[]): boolean => {
                    return items.some((item) => {
                        if (item.expanded) return true;
                        if (item.items) return hasExpanded(item.items);
                        return false;
                    });
                };

                return hasExpanded(menuItems());
            }),

            /**
             * Computed signal that returns all menu items with children (parent items).
             *
             * @returns Array of MenuItem objects that have children
             */
            parentMenuItems: computed(() => {
                const getParents = (items: MenuItem[]): MenuItem[] => {
                    const parents: MenuItem[] = [];
                    items.forEach((item) => {
                        if (item.items && item.items.length > 0) {
                            parents.push(item);
                            parents.push(...getParents(item.items));
                        }
                    });
                    return parents;
                };

                return getParents(menuItems());
            })
        })),
        withMethods((store) => ({
            /**
             * Sets the menu items array.
             *
             * @param menuItems - Array of MenuItem objects
             */
            setMenuItems: (menuItems: MenuItem[]) => {
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
             * Toggles the expanded state of a menu item by ID.
             * Works recursively through nested menu items.
             *
             * @param id - The ID of the menu item to toggle
             */
            toggleMenuItemExpanded: (id: string) => {
                const toggleExpanded = (items: MenuItem[]): MenuItem[] => {
                    return items.map((item) => {
                        if (item.id === id) {
                            return { ...item, expanded: !item.expanded };
                        }
                        if (item.items) {
                            return { ...item, items: toggleExpanded(item.items) };
                        }
                        return item;
                    });
                };

                patchState(store, (state) => ({
                    menuItems: toggleExpanded(state.menuItems)
                }));
            },

            /**
             * Expands a menu item by ID.
             * Works recursively through nested menu items.
             *
             * @param id - The ID of the menu item to expand
             */
            expandMenuItem: (id: string) => {
                const expandItem = (items: MenuItem[]): MenuItem[] => {
                    return items.map((item) => {
                        if (item.id === id) {
                            return { ...item, expanded: true };
                        }
                        if (item.items) {
                            return { ...item, items: expandItem(item.items) };
                        }
                        return item;
                    });
                };

                patchState(store, (state) => ({
                    menuItems: expandItem(state.menuItems)
                }));
            },

            /**
             * Collapses a menu item by ID.
             * Works recursively through nested menu items.
             *
             * @param id - The ID of the menu item to collapse
             */
            collapseMenuItem: (id: string) => {
                const collapseItem = (items: MenuItem[]): MenuItem[] => {
                    return items.map((item) => {
                        if (item.id === id) {
                            return { ...item, expanded: false };
                        }
                        if (item.items) {
                            return { ...item, items: collapseItem(item.items) };
                        }
                        return item;
                    });
                };

                patchState(store, (state) => ({
                    menuItems: collapseItem(state.menuItems)
                }));
            },

            /**
             * Collapses all menu items recursively.
             */
            collapseAllMenuItems: () => {
                const collapseAll = (items: MenuItem[]): MenuItem[] => {
                    return items.map((item) => ({
                        ...item,
                        expanded: false,
                        items: item.items ? collapseAll(item.items) : undefined
                    }));
                };

                patchState(store, (state) => ({
                    menuItems: collapseAll(state.menuItems)
                }));
            },

            /**
             * Updates a specific menu item by ID.
             * Works recursively through nested menu items.
             *
             * @param id - The ID of the menu item to update
             * @param updates - Partial MenuItem object with properties to update
             *
             * @example
             * ```typescript
             * store.updateMenuItem('menu-1', { disabled: true, badge: '5' });
             * ```
             */
            updateMenuItem: (id: string, updates: Partial<MenuItem>) => {
                const updateItem = (items: MenuItem[]): MenuItem[] => {
                    return items.map((item) => {
                        if (item.id === id) {
                            return { ...item, ...updates };
                        }
                        if (item.items) {
                            return { ...item, items: updateItem(item.items) };
                        }
                        return item;
                    });
                };

                patchState(store, (state) => ({
                    menuItems: updateItem(state.menuItems)
                }));
            },

            /**
             * Resets the menu state to initial values.
             */
            resetMenuState: () => {
                patchState(store, initialMenuSlice);
            }
        }))
        // todo: add hooks to load from local storage. This could help us to persist the menu state when refresh the page.
    );
}
