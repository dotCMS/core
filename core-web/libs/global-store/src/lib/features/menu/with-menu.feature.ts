import { tapResponse } from '@ngrx/operators';
import {
    patchState,
    signalStoreFeature,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { addEntities, removeAllEntities, withEntities } from '@ngrx/signals/entities';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { Observable, Subject, of, pipe } from 'rxjs';

import { computed, effect, inject } from '@angular/core';

import { catchError, map, switchMap, take, tap } from 'rxjs/operators';

import { DotLocalstorageService, DotMenuService } from '@dotcms/data-access';
import {
    DotMenu,
    getMenuItemKey,
    MenuGroup,
    MenuItemEntity,
    MenuStatus
} from '@dotcms/dotcms-models';

import { resolveMenuItem } from './menu-resolver';
import { DOT_MENU_ROUTES_REGISTRAR } from './menu-routes-registrar.token';
import { initialMenuSlice, menuConfig } from './menu.slice';

const DOTCMS_MENU_STATUS = 'dotcms.menu.status';

/**
 * Custom Store Feature that owns the user's navigation menu: the one in-memory copy of
 * `GET /api/v1/menu`, its load status and the UI state around it.
 *
 * ## Two kinds of state
 * - **Server snapshot**: the menu item entities and the load `status`. A successful load replaces
 *   the whole snapshot; a failed one never clears what is on screen.
 * - **UI state**: `isNavigationCollapsed` (persisted), `openParentMenuId` and `activeKey`. They are
 *   not deleted when the snapshot is replaced.
 *
 * ## Constraints
 * - An item is identified by its portlet id plus the full layout id.
 * - Only one item is active (`activeKey`) and only one group is open at a time.
 * - Group and item order is the order of the response, which the backend sorts by `tabOrder`.
 */
export function withMenu() {
    return signalStoreFeature(
        withState(initialMenuSlice),
        withEntities(menuConfig),
        withComputed(({ menuItemsEntityMap, menuItemsEntities, openParentMenuId, activeKey }) => {
            /**
             * Menu items grouped by layout, sorted by `tabOrder` and, inside a group, by the item's
             * position. The order does not depend on how the entities are stored.
             *
             * @returns Array of MenuGroup objects with parent information and child items
             */
            const menuGroup = computed((): MenuGroup[] => {
                const currentOpenParentMenuId = openParentMenuId();
                const grouped = new Map<string, MenuItemEntity[]>();

                for (const item of menuItemsEntities()) {
                    const siblings = grouped.get(item.parentMenuId) ?? [];
                    siblings.push(item);
                    grouped.set(item.parentMenuId, siblings);
                }

                return [...grouped.entries()]
                    .map(([parentMenuId, unsorted]) => {
                        const menuItems = [...unsorted].sort((a, b) => a.itemIndex - b.itemIndex);
                        const firstItem = menuItems[0];

                        return {
                            id: parentMenuId,
                            label: firstItem.parentMenuLabel,
                            icon: firstItem.parentMenuIcon,
                            menuItems,
                            isOpen: parentMenuId === currentOpenParentMenuId,
                            tabOrder: firstItem.tabOrder
                        };
                    })
                    .sort((a, b) => a.tabOrder - b.tabOrder);
            });

            /**
             * The highlighted menu item.
             *
             * @returns The active MenuItemEntity or null if no item is active
             */
            const activeMenuItem = computed((): MenuItemEntity | null => {
                const key = activeKey();

                return key === null ? null : (menuItemsEntityMap()[key] ?? null);
            });

            /**
             * Computed signal that returns the entity map for direct lookups.
             *
             * @returns Record of menu items keyed by their entity key
             */
            const entityMap = computed(() => menuItemsEntityMap());

            /**
             * Entity keys, for debugging.
             *
             * @returns Array of entity keys
             */
            const entityKeys = computed(() =>
                menuItemsEntities().map((item) => getMenuItemKey(item))
            );

            /**
             * The first menu item of the first group. Used for initial navigation when loading
             * the application.
             *
             * @returns The first MenuItemEntity or null if no items exist
             */
            const firstMenuItem = computed(() => {
                const groups = menuGroup();
                if (groups.length === 0) return null;

                const firstGroup = groups[0];
                return firstGroup.menuItems.length > 0 ? firstGroup.menuItems[0] : null;
            });

            /** True when the highlighted item exists in the menu. */
            const isGroupActive = computed(() => activeMenuItem() !== null);

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
             * Flattens the menu response into entities and adds what the shell needs: the link the
             * router uses, the short layout id, and the position of the group and of the item.
             */
            const transformMenuToEntities = (menu: DotMenu[]): MenuItemEntity[] => {
                return menu.flatMap((parent, tabOrder) =>
                    parent.menuItems.map((item, itemIndex) => ({
                        ...item,
                        parentMenuId: parent.id,
                        parentMenuLabel: parent.tabName,
                        parentMenuIcon: parent.tabIcon,
                        menuLink: item.angular ? item.url : `/c/${item.id}`,
                        shortLayoutId: parent.id.substring(0, 4),
                        tabOrder,
                        itemIndex
                    }))
                );
            };

            /**
             * Replaces the whole snapshot with a menu. The highlight is kept when its item is still
             * there and cleared when it is gone; the open group and the collapsed state are not
             * touched.
             */
            const replaceSnapshot = (menu: DotMenu[]) => {
                const entities = transformMenuToEntities(menu);
                const activeKey = store.activeKey();
                const keepActive =
                    activeKey !== null &&
                    entities.some((entity) => getMenuItemKey(entity) === activeKey);

                // Clear all entities first, then add new ones
                // This ensures all property changes are detected by signals
                patchState(
                    store,
                    removeAllEntities(menuConfig),
                    addEntities(entities, menuConfig),
                    { activeKey: keepActive ? activeKey : null }
                );
            };

            const menuService = inject(DotMenuService);
            const routesRegistrar = inject(DOT_MENU_ROUTES_REGISTRAR, { optional: true });

            /** Emits every time a load ends, with the status it ended in. */
            const settled$ = new Subject<MenuStatus>();

            /**
             * Registers the runtime routes the menu declares. A registrar that throws must not leave
             * the menu stuck on `loading`, so the failure is reported and the load carries on.
             */
            const registerRoutes = (menus: DotMenu[]) => {
                try {
                    routesRegistrar?.(menus.flatMap((menu) => menu.menuItems));
                } catch (error) {
                    console.error('[menu] could not register the menu routes', error);
                }
            };

            /**
             * Runs the request. `switchMap` keeps only the latest one: a reload that starts while
             * another is in flight cancels it, so a slow old response can never replace a newer menu.
             */
            const runLoad = rxMethod<void>(
                pipe(
                    tap(() => patchState(store, { status: 'loading' })),
                    switchMap(() =>
                        menuService.getMenus().pipe(
                            tapResponse({
                                next: (menus) => {
                                    replaceSnapshot(menus);
                                    // Routes first, `ready` second: whoever waits for the menu can rely on them.
                                    registerRoutes(menus);
                                    patchState(store, { status: 'ready' });
                                    settled$.next('ready');
                                },
                                // Entities already on screen are kept; only the status changes.
                                error: () => {
                                    patchState(store, { status: 'error' });
                                    settled$.next('error');
                                }
                            })
                        )
                    )
                )
            );

            /**
             * Asks the backend for the menu and replaces the snapshot with the answer. It always sends
             * a request and reports `loading` right away, so a caller that checks the status in the
             * same tick sees the load in flight.
             */
            const reload = (): void => {
                runLoad();
            };

            /**
             * Emits once, with `ready` or `error`, when the load that is running (or the next one to
             * run) ends, and completes.
             */
            const whenSettled = (): Observable<MenuStatus> => settled$.pipe(take(1));

            /**
             * Makes sure a menu has been requested, without asking again when it is already there.
             * `ready` completes at once with no request; `loading` waits for the request in flight;
             * `idle` and `error` start one. It settles on `ready` or `error` and never errors, so a
             * caller reads the snapshot afterwards and an empty one means "not in the menu".
             */
            const ensureLoaded = (): Observable<void> =>
                new Observable<void>((subscriber) => {
                    if (store.status() === 'ready') {
                        subscriber.next();
                        subscriber.complete();

                        return undefined;
                    }

                    const subscription = whenSettled().subscribe(() => {
                        subscriber.next();
                        subscriber.complete();
                    });

                    if (store.status() !== 'loading') {
                        reload();
                    }

                    return subscription;
                });

            /**
             * Reads the url of a legacy (non-Angular) portlet from a request made now, because the url
             * carries a timestamp that changes on every request. It never touches the snapshot, the
             * status or the highlight. If the request fails it answers with the url the snapshot has.
             *
             * @param portletId id of the legacy portlet
             * @returns the url, or an empty string when the portlet is not a legacy one in the menu
             */
            const loadFreshPortletUrl = (portletId: string): Observable<string> => {
                // A portlet the menu already knows is an Angular one navigates with its menuLink and has
                // no legacy url to read, so there is nothing to ask for.
                const known = store.menuItemsEntities().filter((item) => item.id === portletId);
                if (known.length > 0 && known.every((item) => item.angular)) {
                    return of('');
                }

                return menuService.getMenus().pipe(
                    map(
                        (menus) =>
                            menus
                                .flatMap((menu) => menu.menuItems)
                                .find((item) => !item.angular && item.id === portletId)?.url ?? ''
                    ),
                    catchError(() =>
                        of(
                            store
                                .menuItemsEntities()
                                .find((item) => !item.angular && item.id === portletId)?.url ?? ''
                        )
                    )
                );
            };

            /**
             * Whether a portlet is in the menu. It reads the snapshot, so it answers once the menu is
             * `ready`; callers that can run earlier call `ensureLoaded()` first.
             *
             * @param portletId - The portlet id to look for, in any layout
             * @param options - `jspOnly`: only count a portlet that has not been migrated to Angular
             */
            const hasPortlet = (
                portletId: string,
                { jspOnly = false }: { jspOnly?: boolean } = {}
            ): boolean =>
                store
                    .menuItemsEntities()
                    .some((item) => item.id === portletId && (!jspOnly || !item.angular));

            /**
             * The full id of the layout that holds a portlet (the first one, when several do).
             *
             * @param portletId - The portlet id to look for
             * @returns The layout id, or `null` when the portlet is not in the menu
             */
            const layoutIdForPortlet = (portletId: string): string | null =>
                store.menuItemsEntities().find((item) => item.id === portletId)?.parentMenuId ??
                null;

            /**
             * Highlights a menu item by its key. A key that is not in the menu is ignored.
             *
             * @param key - The entity key (see `getMenuItemKey`) of the item to highlight
             */
            const activateMenuItem = (key: string) => {
                if (!store.entityMap()[key]) {
                    return;
                }

                patchState(store, { activeKey: key });
            };

            /**
             * Highlights a menu item and opens its group. A key that is not in the menu is ignored.
             *
             * @param menuItemKey - The entity key of the item to highlight
             * @param parentMenuId - The id of the parent menu group to open
             */
            const activateMenuItemWithParent = (
                menuItemKey: string,
                parentMenuId: string | null
            ) => {
                if (!store.entityMap()[menuItemKey]) {
                    return;
                }

                patchState(store, { activeKey: menuItemKey, openParentMenuId: parentMenuId });
            };

            /**
             * Highlights an item, as resolved from a URL, and opens its group.
             *
             * @param item - The resolved item; `null` changes nothing
             */
            const selectMenuItem = (item: MenuItemEntity | null) => {
                if (item) {
                    activateMenuItemWithParent(getMenuItemKey(item), item.parentMenuId);
                }
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
             * Collapses the navigation menu. It does not close the open group.
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
             * Gets the page title for a URL: the label of the menu item the URL resolves to, by the
             * same rule the highlight and the breadcrumbs use.
             *
             * @param url - The current URL to resolve
             * @returns The label of the matching menu item, or empty string if not found
             */
            const getPageTitleByUrl = (url: string): string => {
                return (
                    resolveMenuItem({
                        url,
                        items: store.menuItemsEntities(),
                        openParentMenuId: store.openParentMenuId()
                    })?.label ?? ''
                );
            };

            return {
                reload,
                ensureLoaded,
                whenSettled,
                loadFreshPortletUrl,
                hasPortlet,
                layoutIdForPortlet,
                activateMenuItem,
                activateMenuItemWithParent,
                selectMenuItem,
                toggleParent,
                closeAllParents,
                toggleNavigation,
                collapseNavigation,
                expandNavigation,
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
