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
import { ComponentStatus, DotCMSContentlet, DotContentletCanLock } from '@dotcms/dotcms-models';

import { escapeHtml, resolveLocker } from '../../../utils/functions.util';
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
             * Whether the current user may only read this content.
             *
             * Driven by the backend, not by a frontend permission guess: `_canlock` answers
             * false and the EDITING action list is empty. That is the answer for a user with
             * READ but no EDIT, and also for an EDIT user blocked by someone else's lock: the
             * backend gives both the same response, and neither can save, so both get the
             * disabled form. The layout then shows the lock banner instead of the
             * no-permission message when someone else holds the lock. New content and an
             * unanswered `_canlock` (`null`) are never read-only.
             *
             * @returns boolean - True when the form must render disabled.
             */
            isReadOnly: computed(() => {
                const contentlet = store.contentlet();

                if (!contentlet?.inode || store.initialContentletState() === 'new') {
                    return false;
                }

                if (store.canLock() !== false) {
                    return false;
                }

                return !Object.values(store.currentContentActions()).some(
                    (actions) => actions.length > 0
                );
            }),

            /**
             * Whether the user's edit permission on the current content is known, i.e. the
             * backend has answered `_canlock` (`canLock` is `null` until then). New content has
             * no lock to check, so it counts as settled. The layout keeps its loading screen
             * until this is true, so the form is never rendered enabled and then flipped to
             * read-only on a slow network (#38724 R4).
             *
             * @returns boolean - True when the form can be rendered in its final state.
             */
            isPermissionsSettled: computed(
                () => !store.contentlet()?.identifier || store.canLock() !== null
            ),

            /**
             * Determines if a lock/unlock request is currently in flight
             *
             * @returns boolean - True while the lock or unlock API call is pending
             */
            isLocking: computed(() => store.lockStatus() === ComponentStatus.LOADING),

            /**
             * Generates a user-friendly message about who has locked the content
             *
             * @returns string - Localized message indicating who locked the content or empty string if not locked
             */
            lockWarningMessage: computed((): string | null => {
                const userCanLock = store.canLock();
                const currentUser = store.currentUser();
                const locker = resolveLocker(store.contentlet());

                if (!locker || currentUser?.userId === locker.userId) {
                    return null;
                }

                // The message keys already bold {0} (`<b>{0}.</b>`) and the banner renders via
                // [innerHTML], so escape the API-provided name before interpolating it.
                const safeName = escapeHtml(locker.displayName);

                // If user doesn't have permission to lock, use the no permission message.
                // Fall back to the name-less key when the locker's display name is unavailable.
                if (!userCanLock) {
                    return safeName
                        ? dotMessageService.get('edit.content.locked.no.permission.user', safeName)
                        : dotMessageService.get('edit.content.locked.no.permission');
                }

                // Without a display name, the "Content locked by <b>{name}.</b>" template would
                // render as "Content locked by ." — suppress the banner instead. The lock switch
                // UI still indicates the locked state.
                if (!safeName) {
                    return null;
                }

                return dotMessageService.get('edit.content.locked.by.user', safeName);
            }),

            /**
             * Display name of the user who locked the content, when it is locked by someone
             * other than the current user. Used in the release-lock confirmation dialog.
             *
             * @returns string | null - The locker's display name, or null when not applicable.
             */
            lockedByName: computed((): string | null => {
                const currentUser = store.currentUser();
                const locker = resolveLocker(store.contentlet());

                if (!locker || currentUser?.userId === locker.userId) {
                    return null;
                }

                return locker.displayName || null;
            }),

            /**
             * Determines whether the content is locked by a user other than the current one.
             * Drives the "Release Lock" affordance (vs a plain "Unlock" of the user's own lock).
             *
             * @returns boolean - True when the content is locked by a different user.
             */
            isLockedByAnotherUser: computed(() => {
                const contentlet = store.contentlet();
                const currentUser = store.currentUser();
                const locker = resolveLocker(contentlet);

                if (!contentlet?.locked || !locker) {
                    return false;
                }

                return !!currentUser && currentUser.userId !== locker.userId;
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
                                lockError: null,
                                lockStatus: ComponentStatus.LOADING
                            })
                        ),
                        switchMap(() =>
                            dotContentletService.lockContent(store.contentlet()?.inode).pipe(
                                tapResponse({
                                    next: (updated: DotCMSContentlet) => {
                                        const current = store.contentlet();
                                        if (!current) {
                                            patchState(store, {
                                                lockStatus: ComponentStatus.LOADED
                                            });

                                            return;
                                        }
                                        patchState(store, {
                                            lockStatus: ComponentStatus.LOADED,
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
                                            lockError: error.message,
                                            lockStatus: ComponentStatus.ERROR
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
                                lockError: null,
                                lockStatus: ComponentStatus.LOADING
                            })
                        ),
                        switchMap(() =>
                            dotContentletService.unlockContent(store.contentlet()?.inode).pipe(
                                tapResponse({
                                    next: (updated: DotCMSContentlet) => {
                                        const current = store.contentlet();
                                        if (!current) {
                                            patchState(store, {
                                                lockStatus: ComponentStatus.LOADED
                                            });

                                            return;
                                        }
                                        patchState(store, {
                                            lockStatus: ComponentStatus.LOADED,
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
                                            lockError: error.message,
                                            lockStatus: ComponentStatus.ERROR
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
                                        // A reply for a contentlet that has since been
                                        // replaced (navigation to another item) must not
                                        // settle the new one's permission; its own check
                                        // follows.
                                        if (store.contentlet()?.inode !== contentlet.inode) {
                                            return;
                                        }

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
                                        if (store.contentlet()?.inode !== contentlet.inode) {
                                            return;
                                        }

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
