import { tapResponse } from '@ngrx/operators';
import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, effect, inject, untracked } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotContentletService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { ContentState } from './content.feature';

/**
 * Enum representing the possible states of content locking
 */
export enum LockStatus {
    LOCKED = 'LOCKED',
    UNLOCKED = 'UNLOCKED'
}

export interface LockState {
    lockStatus: LockStatus;
    lockError: string | null;
    canLock: boolean;
}

export const initialLockState: LockState = {
    lockStatus: LockStatus.UNLOCKED,
    lockError: null,
    canLock: false
};

export function withLock() {
    return signalStoreFeature(
        { state: type<ContentState>() },
        withState(initialLockState),

        withComputed((store) => ({
            /**
             * Whether the content is currently locked
             */
            isContentLocked: computed(() => {
                const contentlet = store.contentlet();
                const isCopyingLocale = store.initialContentletState() === 'copy';
                //LockStatus will be removed when the lock/unlock endpoint response is updated.
                const lockStatus = store.lockStatus();

                // console.log('contentlet isContentLocked', contentlet);

                return lockStatus === LockStatus.LOCKED || (contentlet?.locked && !isCopyingLocale);
            })
        })),

        withMethods(
            (
                store,
                dotContentletService = inject(DotContentletService),
                dotHttpErrorManagerService = inject(DotHttpErrorManagerService)
            ) => ({
                /**
                 * Locks the content using the identifier from the current contentlet
                 */
                lockContent: rxMethod<void>(
                    pipe(
                        tap(() =>
                            patchState(store, {
                                lockError: null
                            })
                        ),
                        switchMap(() =>
                            dotContentletService.lockContent(store.contentlet()?.inode).pipe(
                                tapResponse({
                                    next: (_contentlet: DotCMSContentlet) => {
                                        //TODO will update the contentlet in the store when the endpoint response is updated.
                                        patchState(store, {
                                            lockStatus: LockStatus.LOCKED
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        dotHttpErrorManagerService.handle(error);
                                        patchState(store, {
                                            lockError: error.message
                                        });
                                    }
                                })
                            )
                        )
                    )
                ),

                /**
                 * Unlocks the content using the identifier from the current contentlet
                 */
                unlockContent: rxMethod<void>(
                    pipe(
                        tap(() =>
                            patchState(store, {
                                lockError: null
                            })
                        ),
                        switchMap(() =>
                            dotContentletService.unlockContent(store.contentlet()?.inode).pipe(
                                tapResponse({
                                    next: (_contentlet: DotCMSContentlet) => {
                                        patchState(store, {
                                            lockStatus: LockStatus.UNLOCKED
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        dotHttpErrorManagerService.handle(error);
                                        patchState(store, {
                                            lockError: error.message
                                        });
                                    }
                                })
                            )
                        )
                    )
                ),

                /**
                 * Effect to check if content can be locked whenever contentlet changes
                 */
                checkCanLock: rxMethod<void>(
                    pipe(
                        switchMap(() => {
                            const contentlet = store.contentlet();
                            // console.log('contentlet checkCanLock', contentlet);
                            if (!contentlet?.inode) {
                                patchState(store, { canLock: false });

                                return [];
                            }

                            return dotContentletService.canLock(contentlet.inode).pipe(
                                tapResponse({
                                    next: ({ canLock }) => {
                                        // console.log('canLock', canLock);
                                        patchState(store, { canLock });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        dotHttpErrorManagerService.handle(error);
                                        patchState(store, { canLock: false });
                                    }
                                })
                            );
                        })
                    )
                )

                // /**
                //  * Updates the lock state directly
                //  * @param isLocked Whether the content is locked
                //  */
                // updateLockState: (_isLocked: boolean) => {
                //     patchState(store, {
                //         lockStatus: store.isContentLocked()
                //             ? LockStatus.LOCKED
                //             : LockStatus.UNLOCKED
                //     });
                // }
            })
        ),
        withHooks({
            onInit(store) {
                /**
                 * Effect to check if content can be locked whenever contentlet changes
                 */
                effect(() => {
                    const contentlet = store.contentlet();

                    untracked(() => {
                        if (contentlet) {
                            store.checkCanLock();
                        }
                    });
                });
            }
        })
    );
}
