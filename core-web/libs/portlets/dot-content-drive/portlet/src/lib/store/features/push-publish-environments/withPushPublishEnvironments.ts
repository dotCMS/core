import {
    patchState,
    signalStoreFeature,
    type,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';

import { inject } from '@angular/core';

import { take } from 'rxjs/operators';

import { PushPublishService } from '@dotcms/data-access';

import { DotContentDriveState } from '../../../shared/models';

interface WithPushPublishEnvironmentsState {
    /**
     * Whether any push publish environment is reachable by this user's role.
     *
     * `undefined` until the lookup lands, which keeps Push Publish disabled in the meantime rather
     * than enabling it and then retracting. A failed lookup settles on `false` for the same reason:
     * offering a push with nowhere to send it is worse than one disabled row.
     *
     * Held in the store rather than in a component because more than one surface gates on it — the
     * Action Center's Push Publish row and the folder context menu's Push Publish item — and two
     * independent lookups would mean two copies of this three-state handling to keep in step.
     */
    hasPushPublishEnvironments: boolean | undefined;
}

/**
 * Resolves once per portlet load whether Push Publish has anywhere to send to.
 *
 * Gates on **environments reachable by this role**, not on endpoints existing: endpoints can exist
 * with no environment the current role can send to, and that is the case the servlet fails on.
 */
export function withPushPublishEnvironments() {
    return signalStoreFeature(
        {
            state: type<DotContentDriveState>()
        },
        withState<WithPushPublishEnvironmentsState>({
            hasPushPublishEnvironments: undefined
        }),
        withMethods((store, pushPublishService = inject(PushPublishService)) => ({
            /**
             * Looks up the reachable environments and settles the gate.
             *
             * A failure settles on "none", which disables the action. The alternative, treating an
             * unreachable lookup as "probably fine", offers a push that has nowhere to go and fails
             * at the servlet with a message the user cannot act on.
             */
            loadPushPublishEnvironments: (): void => {
                pushPublishService
                    .getEnvironments()
                    .pipe(take(1))
                    .subscribe({
                        next: (environments) =>
                            patchState(store, {
                                hasPushPublishEnvironments: environments.length > 0
                            }),
                        error: () => patchState(store, { hasPushPublishEnvironments: false })
                    });
            }
        })),
        withHooks((store) => {
            return {
                onInit() {
                    // Once per portlet load, not per consumer: the reachable environments are fixed
                    // for the session, and both surfaces that gate on this would otherwise each run
                    // their own lookup. Nothing waits on it — consumers read `undefined` until it
                    // lands, which reads as "disabled".
                    store.loadPushPublishEnvironments();
                }
            };
        })
    );
}
