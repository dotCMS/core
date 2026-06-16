import { tapResponse } from '@ngrx/operators';
import {
    patchState,
    signalStoreFeature,
    withComputed,
    withHooks,
    withMethods,
    withState,
    type
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { Observable, pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotSiteService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotSite } from '@dotcms/dotcms-models';

/**
 * State interface for the Site feature.
 *
 * Holds the currently selected site as returned by DotCMS.
 */
export interface SiteState {
    /**
     * The currently selected site details.
     *
     * Contains the complete site entity from the API endpoint. Set to `null`
     * when no site is selected.
     */
    siteDetails: DotSite | null;
}

/**
 * Initial state for the Site feature.
 */
const initialSiteState: SiteState = {
    siteDetails: null
};

/**
 * Store feature for managing the current site.
 *
 * Owns the `siteDetails` state, the `currentSiteId` computed, and the
 * load/switch/sync site methods. Auto-loads the current site on init and keeps
 * it in sync when another user/tab switches the site (SWITCH_SITE event).
 *
 * ## Dependencies
 * Relies on `switchSiteEvent$()` exposed by `withWebSocket()`, so this feature
 * must be composed *after* `withWebSocket()` in the store.
 *
 * ## Features
 * - Auto-loads the current site on initialization
 * - Exposes `currentSiteId` for services that need site context
 * - `switchCurrentSite()` for explicit user-driven site switches
 * - Syncs `siteDetails` on remote SWITCH_SITE events
 */
export function withSite() {
    return signalStoreFeature(
        // Declares the dependency on the SWITCH_SITE stream provided by withWebSocket().
        {
            methods: type<{
                switchSiteEvent$: () => Observable<DotSite>;
            }>()
        },
        withState(initialSiteState),
        withComputed(({ siteDetails }) => ({
            /**
             * Computed signal that returns the current site identifier.
             *
             * Primary computed for getting the site ID for any services that need
             * site context (analytics, content, etc.).
             *
             * @returns The site identifier string or null if no site is loaded
             */
            currentSiteId: computed(() => siteDetails()?.identifier ?? null)
        })),
        withMethods((store, siteService = inject(DotSiteService)) => ({
            /**
             * Loads the current site from DotCMS and updates the store.
             *
             * Fetches the current site from the DotSiteService and stores it as a
             * complete site entity. Automatically called on initialization, so
             * manual calls are typically not needed.
             */
            loadCurrentSite: rxMethod<void>(
                pipe(
                    switchMap(() =>
                        siteService.getCurrentSite().pipe(
                            tapResponse({
                                next: (siteDetails) => {
                                    patchState(store, { siteDetails });
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
             * Switches the active site and updates the store.
             *
             * Calls DotSiteService.switchSite(), then fetches the now-current site
             * and stores it. Use this when the user explicitly picks a site in the UI.
             */
            switchCurrentSite: rxMethod<string>(
                pipe(
                    switchMap((identifier) =>
                        siteService.switchSite(identifier).pipe(
                            switchMap(() => siteService.getCurrentSite()),
                            tap((siteDetails) => patchState(store, { siteDetails }))
                        )
                    )
                )
            ),

            /**
             * Keeps siteDetails in sync when another user/tab switches the site.
             * Lifetime is tied to the store via rxMethod — no manual teardown needed.
             */
            syncSiteOnSwitchEvent: rxMethod<void>(
                pipe(
                    switchMap(() =>
                        store
                            .switchSiteEvent$()
                            .pipe(tap((site) => patchState(store, { siteDetails: site })))
                    )
                )
            )
        })),
        withHooks((store) => ({
            onInit() {
                const loginService = inject(LoginService);
                // Load the current site reactively from the authentication state instead of
                // a one-shot bootstrap load. The GlobalStore is created at app startup
                // (`provideAppInitializer`), before the session cookie is valid, so a one-shot
                // load would fire pre-auth and never retry after the SPA login navigation —
                // leaving the site selector empty until a manual refresh. `watchUser()` fires
                // when `auth$` emits: on refresh once the AuthGuard resolves auth via
                // `loadAuth()`, and on every login via `setAuth()` (it only fires synchronously
                // if auth was already set before the store initialized). Mirrors the legacy
                // SiteService behavior. See `withUser` for full rationale.
                loginService.watchUser(() => store.loadCurrentSite());
                store.syncSiteOnSwitchEvent();
            }
        }))
    );
}
