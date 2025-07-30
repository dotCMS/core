import { tapResponse } from '@ngrx/operators';
import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withHooks,
    withMethods
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, effect, inject, untracked } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import {
    DotContentletService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotContentletCanLock } from '@dotcms/dotcms-models';

import { EditContentState } from '../../edit-content.store';

export function withLock() {
    return signalStoreFeature(
        { state: type<EditContentState>() },

        withComputed((store, dotMessageService = inject(DotMessageService)) => ({
            /**
             * Determines if the content is currently locked
             *
             * @returns boolean - True if content is locked and not in copy locale mode
             */
            isContentLocked: computed(() => {
                const contentlet = store.contentlet();
                const isCopyingLocale = store.initialContentletState?.() === 'copy';

                return contentlet?.locked && !isCopyingLocale;
            }),

            /**
             * Generates a user-friendly message about who has locked the content
             *
             * @returns string - Localized message indicating who locked the content or empty string if not locked
             */
            lockWarningMessage: computed((): string | null => {
                const userCanLock = store.canLock();
                const currentUser = store.currentUser();
                const contentlet = store.contentlet();

                if (!contentlet) {
                    return null;
                }

                const { lockedBy } = contentlet;

                const isLockedByCurrentUser = currentUser?.userId === lockedBy?.userId;

                // content is not locked or locked by the current user
                if (!lockedBy || isLockedByCurrentUser) {
                    return null;
                }

                const userDisplay = lockedBy.firstName + ' ' + lockedBy.lastName;

                // If user doesn't have permission to lock, use the no permission message
                if (!userCanLock) {
                    return dotMessageService.get(
                        'edit.content.locked.no.permission.user',
                        userDisplay
                    );
                }

                return dotMessageService.get('edit.content.locked.by.user', userDisplay);
            })
        })),

        withMethods(
            (
                store,
                dotContentletService = inject(DotContentletService),
                dotHttpErrorManagerService = inject(DotHttpErrorManagerService)
            ) => ({
                /**
                 * Locks the current content
                 *
                 * Makes an API call to lock the content and updates the store with the returned contentlet
                 * Clears any previous lock errors before attempting to lock
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
                                    next: (contentlet: DotCMSContentlet) => {
                                        patchState(store, {
                                            contentlet
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
                 * Unlocks the current content
                 *
                 * Makes an API call to unlock the content and updates the store with the returned contentlet
                 * Clears any previous lock errors before attempting to unlock
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
                                    next: (contentlet: DotCMSContentlet) => {
                                        patchState(store, {
                                            contentlet
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
                 * Checks if the current content can be locked by the user
                 *
                 * Makes an API call to determine if the user has permission to lock the content
                 * Updates the store with the lock status and appropriate label for the lock switch
                 */
                checkCanLock: rxMethod<void>(
                    pipe(
                        switchMap(() => {
                            const contentlet = store.contentlet();
                            if (!contentlet?.inode) {
                                patchState(store, {
                                    canLock: false,
                                    lockSwitchLabel: 'edit.content.unlocked'
                                });

                                return [];
                            }

                            return dotContentletService.canLock(contentlet.inode).pipe(
                                tapResponse({
                                    next: (response: DotContentletCanLock) => {
                                        // Set the appropriate label based on lock status
                                        const lockSwitchLabel = response.locked
                                            ? 'edit.content.locked'
                                            : 'edit.content.unlocked';

                                        patchState(store, {
                                            canLock: response.canLock,
                                            lockSwitchLabel
                                        });
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
            })
        ),
        withHooks({
            onInit(store) {
                /**
                 * Effect that automatically checks if content can be locked whenever the contentlet changes
                 *
                 * Uses untracked to prevent circular dependencies in the effect
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
