import { patchState, signalStoreFeature, withMethods, withState } from '@ngrx/signals';

import { CurrentUser } from '@dotcms/dotcms-js';

import { UVE_STATUS } from '../../../shared/enums';

/**
 * UVE System State
 *
 * Manages global Universal Visual Editor system state that applies across
 * all features. This includes license capabilities, user context, and overall
 * editor operational status.
 *
 * @property uveStatus - Overall editor operational state (LOADING/LOADED/ERROR)
 * @property uveIsEnterprise - Enterprise license flag (gates premium features)
 * @property uveCurrentUser - Currently authenticated user context
 */
export interface UveState {
    uveStatus: UVE_STATUS;
    uveIsEnterprise: boolean;
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
 * - Store enterprise license flag (used for feature gating)
 * - Maintain current user context (used for permissions)
 *
 * @example
 * ```typescript
 * // In components
 * const isEnterprise = store.uveIsEnterprise();
 * const canUsePremium = isEnterprise && someCondition;
 *
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
            uveIsEnterprise: false,
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
             * Set enterprise license flag
             * Loaded once during initialization from license service
             */
            setUveIsEnterprise(isEnterprise: boolean) {
                patchState(store, { uveIsEnterprise: isEnterprise });
            },

            /**
             * Set current user context
             * Loaded once during initialization from login service
             */
            setUveCurrentUser(currentUser: CurrentUser | null) {
                patchState(store, { uveCurrentUser: currentUser });
            }
        }))
    );
}
