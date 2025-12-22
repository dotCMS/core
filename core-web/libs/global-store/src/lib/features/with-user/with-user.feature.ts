import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, withHooks, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { inject } from '@angular/core';

import { switchMap } from 'rxjs/operators';

import { DotCurrentUserService } from '@dotcms/data-access';
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
        withHooks({
            /**
             * Automatically loads the current user when the feature is initialized.
             */
            onInit(store) {
                // Auto-load current user on feature initialization
                store.loadLoggedUser();
            }
        })
    );
}
