import { patchState, signalStoreFeature, withHooks, withMethods, withState } from '@ngrx/signals';

import { effect, inject } from '@angular/core';

import { CurrentUser } from '@dotcms/dotcms-js';
import { GlobalStore } from '@dotcms/store';

import { UVE_STATUS } from '../../../shared/enums';

/**
 * UVE System State
 *
 * Manages global Universal Visual Editor system state that applies across
 * all features. This includes user context and overall
 * editor operational status.
 *
 * @property uveStatus - Overall editor operational state (LOADING/LOADED/ERROR)
 * @property uveCurrentUser - Currently authenticated user context
 */
export interface UveState {
    uveStatus: UVE_STATUS;
    uveCurrentUser: CurrentUser | null;
}

/**
 * withUve Feature - Global UVE Editor System State
 *
 * This feature manages system-level state that is:
 * - Global to the entire editor (not page-specific)
 * - Loaded during initialization and rarely changes
 * - Used across multiple features (workflow, editor, view)
 * - Scalable for future system-level concerns
 *
 * Responsibilities:
 * - Track overall editor operational status (loading, loaded, error)
 * - Maintain current user context (used for permissions)
 *
 * @example
 * ```typescript
 * // In components
 * // In features
 * if (store.uveStatus() === UVE_STATUS.LOADED) {
 *   // Safe to access data
 * }
 * ```
 */
export function withUve() {
    return signalStoreFeature(
        withState<UveState>({
            uveStatus: UVE_STATUS.LOADING,
            uveCurrentUser: null
        }),
        withMethods((store) => ({
            /**
             * Update the overall editor operational status
             * Used during page loading, saving, and error handling
             */
            setUveStatus(status: UVE_STATUS) {
                patchState(store, { uveStatus: status });
            },

            /**
             * Set current user context
             * Loaded once during initialization from login service
             */
            setUveCurrentUser(currentUser: CurrentUser | null) {
                patchState(store, { uveCurrentUser: currentUser });
            }
        })),
        withHooks({
            /**
             * Keep uveCurrentUser in sync with GlobalStore.loggedUser reactively.
             * Handles late-loaded user (null initially) and changes (logout, login-as).
             */
            onInit(store) {
                const globalStore = inject(GlobalStore);
                effect(() => {
                    const loggedUser = globalStore.loggedUser();
                    store.setUveCurrentUser(loggedUser);
                });
            }
        })
    );
}
