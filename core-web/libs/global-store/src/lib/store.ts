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
import { SiteEntity } from '@dotcms/dotcms-models';

/**
 * Represents the global application state.
 *
 * This interface defines the structure of the global state that is shared
 * across the entire application. It contains user authentication information
 * and can be extended to include other global state properties.
 */
export interface GlobalState {
    /**
     * The currently authenticated user information.
     *
     * Contains the user's name and email address. Set to `null` when no user
     * is authenticated.
     */
    user: { name: string; email: string } | null;

    /**
     * The currently selected site details.
     *
     * Contains the complete site entity from the API endpoint. Set to `null` when no site is selected.
     */
    siteDetails: SiteEntity | null;
}

/**
 * The initial state for the global store.
 *
 * This object represents the default state when the application starts,
 * with no user authenticated.
 */
const initialState: GlobalState = {
    user: null,
    siteDetails: null
};

/**
 * GlobalStore: Global application state using NgRx Signals.
 *
 * This store manages essential global state including user authentication
 * and current site information. It automatically loads the current site on initialization.
 *
 * Current features:
 * - Auto-loads current site on store initialization
 * - Provides currentSiteId computed for any services that need site context
 * - Stores complete site entity from API endpoint
 * - Simple user authentication state
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
 * // Check authentication
 * const isLoggedIn = this.globalStore.isLoggedIn();
 * ```
 */
export const GlobalStore = signalStore(
    { providedIn: 'root' },
    withState(initialState),
    withMethods((store, siteService = inject(DotSiteService)) => {
        return {
            /**
             * Authenticates a user and updates the global state.
             *
             * This method sets the user information in the global store,
             * effectively logging the user into the application.
             *
             * @param user - The user information to authenticate
             * @param user.name - The user's display name
             * @param user.email - The user's email address
             *
             * @example
             * ```typescript
             * this.globalStore.login({
             *   name: 'John Doe',
             *   email: 'john@example.com'
             * });
             * ```
             */
            login(user: { name: string; email: string }) {
                patchState(store, { user });
            },

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
                                next: (site) => {
                                    // Cast to SiteEntity since the API returns the complete site entity
                                    const siteDetails = site as unknown as SiteEntity;
                                    patchState(store, {
                                        siteDetails
                                    });
                                },
                                error: (error) => {
                                    console.error('Error loading current site:', error);
                                    // You could add error state to GlobalState if needed
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
             * @example
             */
            setCurrentSite: (site: SiteEntity) => {
                patchState(store, {
                    siteDetails: site
                });
            }
        };
    }),
    withComputed(({ user, siteDetails }) => ({
        /**
         * Computed signal indicating whether a user is currently authenticated.
         *
         * @returns `true` if user is not null, `false` otherwise
         *
         * @example
         * ```typescript
         * @if (globalStore.isLoggedIn()) {
         *   <span>Welcome, {{ globalStore.user()?.name }}!</span>
         * } @else {
         *   <button>Login</button>
         * }
         * ```
         */
        isLoggedIn: computed(() => user() != null),

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
        currentSiteId: computed(() => siteDetails()?.identifier ?? null)
    })),
    withHooks({
        /**
         * Automatically loads the current site when the store is initialized.
         *
         * This ensures the currentSiteId is available immediately after
         * injecting the store in any component.
         */
        onInit(store) {
            // Load current site on store initialization
            store.loadCurrentSite();
        }
    })
);
