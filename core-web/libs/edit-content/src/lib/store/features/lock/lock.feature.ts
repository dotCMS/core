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

                const { lockedBy, lockedByName } = contentlet;

                if (!lockedBy) {
                    return null;
                }

                // The `lockedBy` field has two shapes depending on the API endpoint:
                // a plain string (userId) when `lockedByName` carries the display name,
                // or a { userId, firstName, lastName } object.
                // TODO: remove this branching once the backend normalizes the shape across content types.
                const isLockedByString = typeof lockedBy === 'string';
                const lockerUserId = isLockedByString ? lockedBy : lockedBy.userId;
                const userDisplay = (
                    isLockedByString
                        ? lockedByName ?? ''
                        : [lockedBy.firstName, lockedBy.lastName].filter(Boolean).join(' ')
                ).trim();

                if (currentUser?.userId === lockerUserId) {
                    return null;
                }

                // If user doesn't have permission to lock, use the no permission message.
                // Fall back to the name-less key when the locker's display name is unavailable.
                if (!userCanLock) {
                    return userDisplay
                        ? dotMessageService.get(
                              'edit.content.locked.no.permission.user',
                              userDisplay
                          )
                        : dotMessageService.get('edit.content.locked.no.permission');
                }

                // Without a display name, the "Content locked by <b>{name}.</b>" template would
                // render as "Content locked by ." — suppress the banner instead. The lock switch
                // UI still indicates the locked state.
                if (!userDisplay) {
                    return null;
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
                                    next: (updated: DotCMSContentlet) => {
                                        const current = store.contentlet();
                                        if (!current) {
                                            return;
                                        }
                                        patchState(store, {
                                            contentlet: {
                                                ...current,
                                                locked: updated.locked,
                                                lockedBy: updated.lockedBy,
                                                lockedByName: updated.lockedByName,
                                                lockedOn: updated.lockedOn
                                            }
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
                                    next: (updated: DotCMSContentlet) => {
                                        const current = store.contentlet();
                                        if (!current) {
                                            return;
                                        }
                                        patchState(store, {
                                            contentlet: {
                                                ...current,
                                                locked: updated.locked,
                                                lockedBy: updated.lockedBy,
                                                lockedByName: updated.lockedByName,
                                                lockedOn: updated.lockedOn
                                            }
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
