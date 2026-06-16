import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, withHooks, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { inject } from '@angular/core';

import { switchMap } from 'rxjs/operators';

import { DotCurrentUserService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotCurrentUser } from '@dotcms/dotcms-models';

/**
 * State interface for the User feature.
 *
 * Holds the current authenticated (logged-in) user as returned by DotCMS.
 */
export interface UserState {
    /**
     * The current authenticated user.
     *
     * This value comes from `DotCurrentUserService.getCurrentUser()` (DotCMS endpoint:
     * `/api/v1/users/current/`). It is `null` until loaded.
     */
    loggedUser: DotCurrentUser | null;
}

/**
 * Initial state for the User feature.
 */
const initialUserState: UserState = {
    loggedUser: null
};

/**
 * Store feature for managing the current authenticated user.
 *
 * Naming note:
 * - We keep the feature name as `withUser()` for consistency with other features like `withSystem()`.
 * - The state property is named `loggedUser` to be explicit that it refers to the *current session user*,
 *   not an arbitrary user record.
 *
 * ## Features
 * - Auto-loads the current user on initialization
 * - Stores the user data in `loggedUser`
 * - Type-safe integration with `DotCurrentUserService`
 *
 */
export function withUser() {
    return signalStoreFeature(
        withState(initialUserState),
        withMethods((store, dotCurrentUserService = inject(DotCurrentUserService)) => ({
            /**
             * Loads the current authenticated user from DotCMS and updates the store.
             *
             * Fetches the user via `DotCurrentUserService.getCurrentUser()` and patches `loggedUser`.
             * This method is automatically called on feature initialization.
             */
            loadLoggedUser: rxMethod<void>(
                pipe(
                    switchMap(() =>
                        dotCurrentUserService.getCurrentUser().pipe(
                            tapResponse({
                                next: (loggedUser) => {
                                    patchState(store, {
                                        loggedUser
                                    });
                                },
                                error: (error) => {
                                    console.warn('[withUser] Error loading logged user:', error);
                                }
                            })
                        )
                    )
                )
            )
        })),
        withHooks((store) => ({
            /**
             * Reactively (re)loads the current user from the authentication state.
             *
             * Why not a one-shot load here:
             * The GlobalStore is instantiated at app bootstrap (`provideAppInitializer`),
             * before the session cookie is valid (e.g. while on the login page). A one-shot
             * load in `onInit` would therefore fire pre-auth, fail, and never retry — and
             * because login navigates via the SPA router (no full page reload), the user
             * would stay `null` until a manual refresh.
             *
             * `loginService.watchUser()` fires the callback synchronously only if auth was
             * already established before the store initialized; otherwise it fires when
             * `auth$` emits. In practice that means: on a hard refresh it fires once the
             * AuthGuard resolves auth via `loadAuth()` (the store subscribes at bootstrap,
             * before the guard runs), and on every login it fires when `setAuth()` emits — so
             * the user is loaded for the initial login and subsequent re-logins alike. This
             * mirrors the reactive pattern the legacy `SiteService` used
             * (`loginService.watchUser(() => this.loadCurrentSite())`).
             */
            onInit() {
                const loginService = inject(LoginService);
                loginService.watchUser(() => store.loadLoggedUser());
            }
        }))
    );
}
