import { tapResponse } from '@ngrx/operators';
import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { switchMap } from 'rxjs/operators';

import { DotSiteService } from '@dotcms/data-access';
import { DotMenu, DotMenuItem, SiteEntity } from '@dotcms/dotcms-models';

import { withBreadcrumbs } from './features/breadcrumb/breadcrumb.feature';
import { withMenu } from './features/menu/with-menu.feature';
import { withSystem } from './features/with-system/with-system.feature';

/**
 * Represents the global application state.
 *
 * This interface defines the structure of the global state that is shared
 * across the entire application. It contains user authentication information
 * and can be extended to include other global state properties.
 */
export interface GlobalState {
    /**
     * The currently selected site details.
     *
     * Contains the complete site entity from the API endpoint. Set to `null` when no site is selected.
     */
    siteDetails: SiteEntity | null;
    menuItemsTemp: DotMenu[];
}

/**
 * The initial state for the global store.
 *
 * This object represents the default state when the application starts,
 * with no site selected.
 */
const initialState: GlobalState = {
    siteDetails: null,
    menuItemsTemp: []
};

/**
 * GlobalStore: Global application state using NgRx Signals.
 *
 * This store manages essential global state including user authentication,
 * current site information, and system configuration. It uses the custom
 * `withSystem` feature for system configuration management.
 *
 * Current features:
 * - Auto-loads current site on store initialization
 * - Provides currentSiteId computed for any services that need site context
 * - Stores complete site entity from API endpoint
 * - Includes withSystem feature for system configuration management
 * - Includes withMenu feature for menu state management
 *
 * Example usage:
 * ```typescript
 * // Inject the store in a component
 * private readonly globalStore = inject(GlobalStore);
 *
 * // Get current site ID
 * const siteId = this.globalStore.currentSiteId();
 *
 * // Use site ID in your services
 * if (siteId) {
 *   this.someService.doSomething(siteId);
 * }
 *
 * // Access complete site entity
 * const site = this.globalStore.siteDetails();
 * console.log(site?.name, site?.hostname);
 *
 */
export const GlobalStore = signalStore(
    { providedIn: 'root' },
    withState(initialState),
    withSystem(),
    withBreadcrumbs(),
    withMenu(),
    withMethods((store, siteService = inject(DotSiteService)) => {
        return {
            /**
             * Loads the current site from DotCMS and updates the global store.
             *
             * Fetches the current site from the DotSiteService and stores it as a complete
             * SiteEntity in the global state. This method is automatically called on store
             * initialization, so manual calls are typically not needed.
             *
             * @example
             * ```typescript
             * // Manual call (usually not needed)
             * this.globalStore.loadCurrentSite();
             * ```
             */
            loadCurrentSite: rxMethod<void>(
                pipe(
                    switchMap(() =>
                        siteService.getCurrentSite().pipe(
                            tapResponse({
                                next: (siteDetails) => {
                                    patchState(store, {
                                        siteDetails
                                    });
                                },
                                error: (error) => {
                                    // TODO: Define a better error handling strategy for global store
                                    console.warn('Error loading current site:', error);
                                }
                            })
                        )
                    )
                )
            ),

            /**
             * Sets the current site in the global store.
             *
             * This method updates the siteDetails property in the global state
             * with the provided SiteEntity.
             *
             * @param site - The SiteEntity to set as the current site
             *
             */
            setCurrentSite: (site: SiteEntity) => {
                patchState(store, {
                    siteDetails: site
                });
            },
            setMenuItemsTemp: (menuItemsTemp: DotMenu[]) => {
                patchState(store, {
                    menuItemsTemp
                });
            }
        };
    }),
    withComputed(({ siteDetails, menuItemsTemp }) => ({
        /**
         * Computed signal that returns the current site identifier.
         *
         * This is the primary computed for getting the site ID for any services
         * that need site context (analytics, content, etc.).
         * Returns the identifier from the loaded site entity.
         *
         * @returns The site identifier string or null if no site is loaded
         *
         * @example
         * ```typescript
         * const siteId = this.globalStore.currentSiteId();
         * if (siteId) {
         *   this.myService.fetchData(siteId);
         * }
         * ```
         */
        currentSiteId: computed(() => siteDetails()?.identifier ?? null),
        /**
         * Computed signal that returns the flattened menu items.
         *
         * This is the computed for getting the flattened menu items for the breadcrumb.
         * Returns the flattened menu items from the menuItems.
         *
         * @returns The flattened menu items
         */
        flattenMenuItems: computed(() => {
            const menu = menuItemsTemp();
            return menu.reduce<Array<DotMenuItem & { labelParent: string }>>(
                (acc, menu: DotMenu) => {
                    const items = menu.menuItems.map((item: DotMenuItem) => ({
                        ...item,
                        labelParent: menu.tabName
                    }));
                    return [...acc, ...items];
                },
                []
            );
        })
    })),
    withHooks({
        /**
         * Automatically loads the current site when the store is initialized.
         *
         * The system configuration is automatically loaded by the withSystem feature.
         * This ensures the currentSiteId is available immediately after injecting
         * the store in any component.
         */
        onInit(store) {
            // Load current site on store initialization
            // System configuration is automatically loaded by withSystem feature
            store.loadCurrentSite();
        }
    })
);
