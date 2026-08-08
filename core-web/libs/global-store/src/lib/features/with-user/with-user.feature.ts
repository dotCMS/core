import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, withHooks, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { distinctUntilChanged, filter, map, startWith, switchMap } from 'rxjs/operators';

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
             * Instead we react to `loginService.auth$`: `startWith(loginService.auth)` seeds
             * the stream with the current auth (already-established session, e.g. lazy store
             * init after login), then `auth$` delivers future logins. On a hard refresh the
             * emission comes once the AuthGuard resolves auth via `loadAuth()` → `setAuth()`
             * (the store subscribes at bootstrap, before the guard runs); on login it comes
             * from `setAuth()`. We key on `user.userId` and `distinctUntilChanged()` so
             * re-emissions that don't change the user (e.g. login-as, which only sets
             * `loginAsUser`) don't trigger a redundant reload.
             */
            onInit() {
                const loginService = inject(LoginService);
                const destroyRef = inject(DestroyRef);
                loginService.auth$
                    .pipe(
                        startWith(loginService.auth),
                        map((auth) => auth?.user?.userId ?? null),
                        filter((userId): userId is string => !!userId),
                        distinctUntilChanged(),
                        // Tie the subscription to the store's lifecycle so it is torn down if
                        // the store is ever destroyed (e.g. if this feature is reused in a
                        // non-root, scoped store). Harmless for the current root singleton.
                        takeUntilDestroyed(destroyRef)
                    )
                    .subscribe(() => store.loadLoggedUser());
            }
        }))
    );
}
