import { tapResponse } from '@ngrx/operators';
import {
    patchState,
    signalStoreFeature,
    type,
    withMethods,
    withHooks,
    withComputed
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject, effect, untracked, computed } from '@angular/core';

import { ConfirmationService, MessageService } from 'primeng/api';

import { switchMap, tap } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotVersionableService,
    DotMessageService,
    DotContentletService
} from '@dotcms/data-access';
import { ComponentStatus, DotCMSContentletVersion, DotPagination } from '@dotcms/dotcms-models';

import { ContentletIdentifier } from '../../../models/dot-edit-content-field.type';
import {
    DotHistoryTimelineItemAction,
    DotHistoryTimelineItemActionType,
    DotPushPublishHistoryItem
} from '../../../models/dot-edit-content.model';
import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { EDIT_CONTENT_HOST } from '../../../services/host/edit-content-host.model';
import { EditContentState } from '../../edit-content.store';

/**
 * Default number of items per page for versions pagination. Need 40 so scroll works correctly in large viewports.
 */
export const DEFAULT_VERSIONS_PER_PAGE = 40;

/**
 * Default number of items per page for push publish history pagination.
 */
export const DEFAULT_PUSH_PUBLISH_HISTORY_PER_PAGE = 40;

/**
 * Version-map key of the system default locale, used as compare fallback
 * before the current locale is loaded.
 */
export const DEFAULT_LOCALE_ISO_KEY = 'en-us';

/**
 * Index of the History tab in the sidebar tab list. A compare session only
 * makes sense while this tab is active — leaving it exits compare view.
 */
export const HISTORY_SIDEBAR_TAB_INDEX = 1;

/**
 * Feature store for managing content versions state
 */
export function withHistory() {
    return signalStoreFeature(
        { state: type<EditContentState>() },
        withComputed((store) => ({
            compareData: computed(() => {
                // Derive the version-map key from the currently selected locale so the
                // "current" side of the diff reflects the active locale, not the default one.
                // Matches backend Language.getIsoCode(): lowercase `languageCode-countryCode`
                // (or just `languageCode` when there is no country code).
                const compareContentlet = store.compareContentlet();

                if (!compareContentlet) {
                    return null;
                }

                const locale = store.currentLocale();
                const language = locale
                    ? (
                          locale.isoCode ||
                          (locale.countryCode
                              ? `${locale.languageCode}-${locale.countryCode}`
                              : locale.languageCode)
                      ).toLowerCase()
                    : DEFAULT_LOCALE_ISO_KEY;

                return {
                    inode: compareContentlet.inode,
                    identifier: compareContentlet.identifier,
                    language
                };
            })
        })),
        withMethods(
            (
                store,
                dotEditContentService = inject(DotEditContentService),
                errorManager = inject(DotHttpErrorManagerService),
                confirmationService = inject(ConfirmationService),
                dotVersionableService = inject(DotVersionableService),
                messageService = inject(MessageService),
                dotMessageService = inject(DotMessageService),
                dotContentletService = inject(DotContentletService),
                host = inject(EDIT_CONTENT_HOST)
            ) => {
                /**
                 * Deletes a version by inode and reloads the versions list reactively
                 * @param inode - The inode of the version to delete
                 */
                const deleteVersion = rxMethod<string>(
                    pipe(
                        switchMap((inode) =>
                            dotVersionableService.deleteVersion(inode).pipe(
                                tap(() => {
                                    // Show success notification immediately after deletion
                                    messageService.add({
                                        severity: 'success',
                                        summary: dotMessageService.get('Success'),
                                        detail: dotMessageService.get(
                                            'edit.content.sidebar.history.version.deleted.successfully'
                                        )
                                    });

                                    // Set loading state for versions reload
                                    patchState(store, {
                                        versionsStatus: {
                                            status: ComponentStatus.LOADING,
                                            error: null
                                        }
                                    });
                                }),
                                switchMap(() => {
                                    // Chain the versions reload after successful deletion
                                    const contentlet = store.contentlet();
                                    if (contentlet?.identifier) {
                                        return dotEditContentService.getVersions(
                                            contentlet.identifier,
                                            { offset: 1, limit: DEFAULT_VERSIONS_PER_PAGE },
                                            contentlet.languageId
                                        );
                                    }
                                    // Return empty observable if no contentlet identifier
                                    return [];
                                }),
                                tapResponse({
                                    next: (response) => {
                                        patchState(store, {
                                            versions: response.entity,
                                            versionsPagination:
                                                response.pagination as DotPagination,
                                            versionsStatus: {
                                                status: ComponentStatus.LOADED,
                                                error: null
                                            }
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        // Handle both deletion and reload errors
                                        patchState(store, {
                                            versionsStatus: {
                                                status: ComponentStatus.ERROR,
                                                error: error.message
                                            }
                                        });
                                        errorManager.handle(error);
                                    }
                                })
                            )
                        )
                    )
                );

                const restoreVersion = rxMethod<string>(
                    pipe(
                        switchMap((inode) => {
                            const currentContentlet = store.originalContentlet();

                            return dotVersionableService.bringBack(inode).pipe(
                                tapResponse({
                                    next: (restoredVersion) => {
                                        // Navigate to the restored version. The host decides how:
                                        // the full-screen host navigates when the inode changed;
                                        // the dialog host stays in place (no-op).
                                        host.goToRestoredVersion(
                                            restoredVersion.inode,
                                            currentContentlet?.inode
                                        );
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        // Handle restoration errors
                                        errorManager.handle(error);
                                    }
                                })
                            );
                        })
                    )
                );

                /**
                 * Shows restore confirmation dialog and executes restore if confirmed
                 * @param inode - The inode of the version to restore
                 */
                const confirmAndRestoreVersion = (inode: string) => {
                    confirmationService.confirm({
                        message: dotMessageService.get(
                            'edit.content.sidebar.history.restore.confirm.message'
                        ),
                        header: dotMessageService.get(
                            'edit.content.sidebar.history.restore.confirm.header'
                        ),
                        acceptLabel: dotMessageService.get(
                            'edit.content.sidebar.history.restore.confirm.accept'
                        ),
                        rejectLabel: dotMessageService.get(
                            'edit.content.sidebar.history.restore.confirm.reject'
                        ),
                        acceptIcon: 'hidden',
                        rejectIcon: 'hidden',
                        rejectButtonStyleClass: 'p-button-outlined',
                        accept: () => {
                            restoreVersion(inode);
                        }
                    });
                };

                /**
                 * Loads content for a specific version by inode for historical viewing
                 * @param inode - The inode of the version to load
                 */
                const loadVersionContent = rxMethod<string>(
                    pipe(
                        tap((inode) => patchState(store, { loadingVersionInode: inode })),
                        switchMap((inode) =>
                            dotContentletService.getContentletByInode(inode).pipe(
                                tapResponse({
                                    next: (versionContent) => {
                                        const currentContentlet = store.contentlet();
                                        patchState(store, {
                                            uiState: {
                                                ...store.uiState(),
                                                view: 'form'
                                            },
                                            // Store original contentlet if not already stored
                                            originalContentlet: store.isViewingHistoricalVersion()
                                                ? store.originalContentlet()
                                                : currentContentlet,
                                            // Set the historical version as current
                                            contentlet: versionContent,
                                            compareContentlet: null,
                                            isViewingHistoricalVersion: true,
                                            historicalVersionInode: inode,
                                            loadingVersionInode: null
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        // Handle load errors - show error toast and maintain current version
                                        patchState(store, { loadingVersionInode: null });
                                        errorManager.handle(error);
                                        messageService.add({
                                            severity: 'error',
                                            summary: dotMessageService.get('Error'),
                                            detail: dotMessageService.get(
                                                'edit.content.sidebar.history.load.error'
                                            )
                                        });
                                    }
                                })
                            )
                        )
                    )
                );

                /**
                 * Exits historical version view and returns to original content
                 */
                const exitHistoricalView = () => {
                    const originalContentlet = store.originalContentlet();
                    if (originalContentlet) {
                        patchState(store, {
                            contentlet: originalContentlet,
                            isViewingHistoricalVersion: false,
                            historicalVersionInode: null,
                            originalContentlet: null
                        });
                    }
                };

                /**
                 * Exits compare view and returns to the original content / form view.
                 * Clears compare-related state without affecting other tabs.
                 */
                const exitCompareView = () => {
                    const originalContentlet = store.originalContentlet();
                    patchState(store, {
                        compareContentlet: null,
                        historicalVersionInode: null,
                        isViewingHistoricalVersion: false,
                        ...(originalContentlet
                            ? { contentlet: originalContentlet, originalContentlet: null }
                            : {}),
                        uiState: { ...store.uiState(), view: 'form' }
                    });
                };

                const loadCompareVersionContent = rxMethod<string>(
                    pipe(
                        tap((inode) => patchState(store, { loadingVersionInode: inode })),
                        switchMap((inode) =>
                            dotContentletService.getContentletByInode(inode).pipe(
                                tapResponse({
                                    next: (versionContent) => {
                                        const currentContentlet = store.contentlet();
                                        patchState(store, {
                                            compareContentlet: versionContent,
                                            uiState: {
                                                ...store.uiState(),
                                                view: 'compare'
                                            },
                                            // Store original contentlet if not already stored
                                            originalContentlet: store.isViewingHistoricalVersion()
                                                ? store.originalContentlet()
                                                : currentContentlet,
                                            isViewingHistoricalVersion: false,
                                            historicalVersionInode: inode,
                                            loadingVersionInode: null
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        // Handle load errors - show error toast and maintain current version
                                        patchState(store, { loadingVersionInode: null });
                                        errorManager.handle(error);
                                        messageService.add({
                                            severity: 'error',
                                            summary: dotMessageService.get('Error'),
                                            detail: dotMessageService.get(
                                                'edit.content.sidebar.history.load.error'
                                            )
                                        });
                                    }
                                })
                            )
                        )
                    )
                );

                return {
                    /**
                     * Loads content versions with intelligent pagination and accumulation
                     *
                     * This method automatically handles:
                     * - Initial loading (page 1 or new content): Replaces all versions
                     * - Infinite scroll accumulation (page 2+): Appends new versions
                     * - Loading states: Shows loading only on initial load
                     * - Assumes endpoint provides unique items per page
                     *
                     * @param params Object containing identifier and page number
                     * @param params.identifier - Content identifier to load versions for
                     * @param params.page - Page number (1 for initial load, 2+ for infinite scroll)
                     */
                    loadVersions: rxMethod<{ identifier: ContentletIdentifier; page: number }>(
                        pipe(
                            tap(({ page }) => {
                                // Only show loading on initial load (page 1)
                                if (page === 1) {
                                    patchState(store, {
                                        uiState: {
                                            ...store.uiState(),
                                            view: 'form'
                                        },
                                        versionsStatus: {
                                            status: ComponentStatus.LOADING,
                                            error: null
                                        }
                                    });
                                }
                            }),
                            switchMap(({ identifier, page }) => {
                                const currentPagination = store.versionsPagination();
                                const currentVersions = store.versions();
                                const contentlet = store.contentlet();
                                const limit =
                                    currentPagination?.perPage || DEFAULT_VERSIONS_PER_PAGE;

                                // Detect if we're switching content or starting fresh
                                const isNewContent =
                                    currentPagination === null || currentVersions.length === 0;

                                return dotEditContentService
                                    .getVersions(
                                        identifier,
                                        { offset: page, limit },
                                        contentlet?.languageId
                                    )
                                    .pipe(
                                        tapResponse({
                                            next: (response) => {
                                                let newVersions: DotCMSContentletVersion[];

                                                // Logic for accumulation:
                                                // 1. If new content OR page 1: reset (initial load)
                                                // 2. Otherwise: accumulate items (endpoint guarantees no duplicates)
                                                if (isNewContent || page === 1) {
                                                    newVersions = response.entity;
                                                } else {
                                                    // Accumulate: append new items directly
                                                    newVersions = [
                                                        ...currentVersions,
                                                        ...response.entity
                                                    ];
                                                }

                                                patchState(store, {
                                                    uiState: {
                                                        ...store.uiState(),
                                                        view: 'form'
                                                    },
                                                    versions: newVersions, // All accumulated items for display
                                                    versionsPagination:
                                                        response.pagination as DotPagination,
                                                    versionsStatus: {
                                                        status: ComponentStatus.LOADED,
                                                        error: null
                                                    }
                                                });
                                            },
                                            error: (error: HttpErrorResponse) => {
                                                errorManager.handle(error);
                                                patchState(store, {
                                                    versionsStatus: {
                                                        status: ComponentStatus.ERROR,
                                                        error: error.message
                                                    }
                                                });
                                            }
                                        })
                                    );
                            })
                        )
                    ),

                    /**
                     * Loads push publish history with intelligent pagination and accumulation
                     *
                     * This method automatically handles:
                     * - Initial loading (page 1 or new content): Replaces all push publish history
                     * - Infinite scroll accumulation (page 2+): Appends new push publish history
                     * - Loading states: Shows loading only on initial load
                     * - Assumes endpoint provides unique items per page
                     *
                     * @param params Object containing identifier and page number
                     * @param params.identifier - Content identifier to load push publish history for
                     * @param params.page - Page number (1 for initial load, 2+ for infinite scroll)
                     */
                    loadPushPublishHistory: rxMethod<{
                        identifier: ContentletIdentifier;
                        page: number;
                    }>(
                        pipe(
                            tap(({ page }) => {
                                // Only show loading on initial load (page 1)
                                if (page === 1) {
                                    patchState(store, {
                                        pushPublishHistoryStatus: {
                                            status: ComponentStatus.LOADING,
                                            error: null
                                        }
                                    });
                                }
                            }),
                            switchMap(({ identifier, page }) => {
                                const currentPagination = store.pushPublishHistoryPagination();
                                const currentPushPublishHistory = store.pushPublishHistory();
                                const limit =
                                    currentPagination?.perPage ||
                                    DEFAULT_PUSH_PUBLISH_HISTORY_PER_PAGE;

                                // Detect if we're switching content or starting fresh
                                const isNewContent =
                                    currentPagination === null ||
                                    currentPushPublishHistory.length === 0;

                                return dotEditContentService
                                    .getPushPublishHistory(identifier, { offset: page, limit })
                                    .pipe(
                                        tapResponse({
                                            next: (response) => {
                                                let newPushPublishHistory: DotPushPublishHistoryItem[];

                                                // Logic for accumulation:
                                                // 1. If new content OR page 1: reset (initial load)
                                                // 2. Otherwise: accumulate items (endpoint guarantees no duplicates)
                                                if (isNewContent || page === 1) {
                                                    newPushPublishHistory = response.entity;
                                                } else {
                                                    // Accumulate: append new items directly
                                                    newPushPublishHistory = [
                                                        ...currentPushPublishHistory,
                                                        ...response.entity
                                                    ];
                                                }

                                                // Ensure newest-first ordering regardless of API order
                                                newPushPublishHistory = [
                                                    ...newPushPublishHistory
                                                ].sort((a, b) => b.pushDate - a.pushDate);

                                                patchState(store, {
                                                    pushPublishHistory: newPushPublishHistory, // All accumulated items for display
                                                    pushPublishHistoryPagination:
                                                        response.pagination as DotPagination,
                                                    pushPublishHistoryStatus: {
                                                        status: ComponentStatus.LOADED,
                                                        error: null
                                                    }
                                                });
                                            },
                                            error: (error: HttpErrorResponse) => {
                                                errorManager.handle(error);
                                                patchState(store, {
                                                    pushPublishHistoryStatus: {
                                                        status: ComponentStatus.ERROR,
                                                        error: error.message
                                                    }
                                                });
                                            }
                                        })
                                    );
                            })
                        )
                    ),

                    /**
                     * Resets versions to empty array
                     * Useful when switching content or starting fresh
                     */
                    resetVersions: () => {
                        patchState(store, {
                            versions: []
                        });
                    },

                    /**
                     * Resets push publish history to empty array
                     * Useful when switching content or starting fresh
                     */
                    resetPushPublishHistory: () => {
                        patchState(store, {
                            pushPublishHistory: []
                        });
                    },

                    /**
                     * Clears the versions data and resets status to initial state
                     */
                    clearVersions: () => {
                        patchState(store, {
                            versions: [],
                            versionsPagination: null,
                            versionsStatus: {
                                status: ComponentStatus.INIT,
                                error: null
                            }
                        });
                    },

                    /**
                     * Clears the push publish history data and resets status to initial state
                     */
                    clearPushPublishHistory: () => {
                        patchState(store, {
                            pushPublishHistory: [],
                            pushPublishHistoryPagination: null,
                            pushPublishHistoryStatus: {
                                status: ComponentStatus.INIT,
                                error: null
                            }
                        });
                    },

                    /**
                     * Deletes all push publish history for a content item
                     * Shows confirmation dialog and clears local state on success
                     * @param identifier - The content identifier
                     */
                    deletePushPublishHistory: (identifier: string) => {
                        confirmationService.confirm({
                            message: dotMessageService.get(
                                'edit.content.sidebar.history.push.publish.delete.all.confirm.message'
                            ),
                            header: dotMessageService.get(
                                'edit.content.sidebar.history.push.publish.delete.all.confirm.header'
                            ),
                            icon: 'pi pi-exclamation-triangle text-warning-yellow',
                            acceptLabel: dotMessageService.get('delete'),
                            rejectLabel: dotMessageService.get('cancel'),
                            acceptIcon: 'hidden',
                            rejectIcon: 'hidden',
                            rejectButtonStyleClass: 'p-button-outlined',
                            accept: () => {
                                patchState(store, {
                                    pushPublishHistoryStatus: {
                                        status: ComponentStatus.LOADING,
                                        error: null
                                    }
                                });

                                dotEditContentService
                                    .deletePushPublishHistory(identifier)
                                    .subscribe({
                                        next: () => {
                                            // Clear the push publish history data on successful deletion
                                            patchState(store, {
                                                pushPublishHistory: [],
                                                pushPublishHistoryPagination: null,
                                                pushPublishHistoryStatus: {
                                                    status: ComponentStatus.LOADED,
                                                    error: null
                                                }
                                            });

                                            // Show success message
                                            messageService.add({
                                                severity: 'success',
                                                summary: dotMessageService.get('success'),
                                                detail: dotMessageService.get(
                                                    'edit.content.sidebar.history.push.publish.delete.all.success'
                                                )
                                            });
                                        },
                                        error: (error) => {
                                            errorManager.handle(error);
                                            patchState(store, {
                                                pushPublishHistoryStatus: {
                                                    status: ComponentStatus.ERROR,
                                                    error: error.message
                                                }
                                            });

                                            // Show error message
                                            messageService.add({
                                                severity: 'error',
                                                summary: dotMessageService.get('error'),
                                                detail: dotMessageService.get(
                                                    'edit.content.sidebar.history.push.publish.delete.all.error'
                                                )
                                            });
                                        }
                                    });
                            }
                        });
                    },

                    /**
                     * Exposes the delete version method for external use
                     */
                    deleteVersion: deleteVersion,

                    /**
                     * Exposes the restore version method for external use
                     */
                    restoreVersion: restoreVersion,

                    /**
                     * Handles history timeline item actions
                     * Centralizes all version-related actions for reusability across components
                     *
                     * @param action - The action object containing type and item data
                     */
                    handleHistoryAction: (action: DotHistoryTimelineItemAction) => {
                        switch (action.type) {
                            case DotHistoryTimelineItemActionType.VIEW: {
                                const isCompareView = store.uiState().view === 'compare';

                                if (isCompareView) {
                                    if (action.item.working) {
                                        // Clicking the current version while comparing exits compare
                                        exitCompareView();
                                    } else {
                                        // Keep the compare layout and diff against the clicked version
                                        loadCompareVersionContent(action.item.inode);
                                    }
                                } else if (action.item.working) {
                                    // If clicking on working version, exit historical view
                                    exitHistoricalView();
                                } else {
                                    // If clicking on historical version, load it
                                    loadVersionContent(action.item.inode);
                                }
                                break;
                            }
                            case DotHistoryTimelineItemActionType.PREVIEW:
                                // TODO: Implement preview functionality

                                break;
                            case DotHistoryTimelineItemActionType.RESTORE:
                                confirmAndRestoreVersion(action.item.inode);
                                break;
                            case DotHistoryTimelineItemActionType.COMPARE:
                                loadCompareVersionContent(action.item.inode);
                                break;
                            case DotHistoryTimelineItemActionType.DELETE:
                                confirmationService.confirm({
                                    message: dotMessageService.get(
                                        'edit.content.sidebar.history.delete.confirm.message'
                                    ),
                                    header: dotMessageService.get(
                                        'edit.content.sidebar.history.delete.confirm.header'
                                    ),
                                    acceptLabel: dotMessageService.get(
                                        'edit.content.sidebar.history.delete.confirm.accept'
                                    ),
                                    rejectLabel: dotMessageService.get(
                                        'edit.content.sidebar.history.delete.confirm.reject'
                                    ),
                                    acceptIcon: 'hidden',
                                    rejectIcon: 'hidden',
                                    rejectButtonStyleClass: 'p-button-outlined',
                                    accept: () => {
                                        deleteVersion(action.item.inode);
                                    }
                                });
                                break;
                        }
                    },

                    /**
                     * Loads content for a specific version by inode
                     */
                    loadVersionContent: loadVersionContent,

                    /**
                     * Exits historical version view and returns to original content
                     */
                    exitHistoricalView: exitHistoricalView,

                    /**
                     * Exits compare view and returns to the form view
                     */
                    exitCompareView: exitCompareView,

                    /**
                     * Restores the currently viewed historical version with confirmation
                     */
                    restoreCurrentHistoricalVersion: () => {
                        const historicalInode = store.historicalVersionInode();
                        if (historicalInode) {
                            confirmAndRestoreVersion(historicalInode);
                        }
                    },

                    /**
                     * Shows restore confirmation dialog and executes restore if confirmed
                     */
                    confirmAndRestoreVersion: confirmAndRestoreVersion
                };
            }
        ),
        withHooks({
            onInit(store) {
                /**
                 * Effect that reloads the history datasets when the contentlet changes,
                 * scoped to what each dataset actually depends on:
                 * - Versions are per identifier + language, so viewing another version
                 *   (same content, same locale) does not refetch them.
                 * - Push publish history is per identifier only, so it never reloads on
                 *   version or locale switches.
                 * Reloads never clear the current items first: the previous list stays
                 * visible while loading (page 1 replaces it on response), so the sidebar
                 * doesn't collapse into skeletons.
                 *
                 * The identity keys alone are not enough: a save/publish mints a NEW inode
                 * under the SAME identifier and locale, so the keys stay equal while the
                 * version list has genuinely gone stale. Two extra signals cover that:
                 * - A list emptied back to INIT by `initializeExistingContent` (the
                 *   full-screen in-place reload), which would otherwise render empty.
                 * - The live inode moving while NOT browsing history (the dialog host never
                 *   re-initializes, so nothing clears or refetches on its own).
                 * `loadedLiveInode` only tracks the inode of the live version, so entering
                 * a historical/compare version — and returning from it — does not refetch.
                 */
                let loadedVersionsKey: string | null = null;
                let loadedLiveInode: string | null = null;
                let loadedPushPublishIdentifier: string | null = null;

                effect(() => {
                    const contentlet = store.contentlet();
                    // An in-place reload empties the lists back to INIT while deliberately
                    // keeping the OUTGOING contentlet on screen (stale-while-revalidate),
                    // so acting on the cleared flag mid-reload would fetch the identifier
                    // we are leaving. Waiting for LOADING to clear is safe: the reload
                    // patches `contentlet` and `state: LOADED` together while the statuses
                    // are still INIT, so this fires on that same pass.
                    //
                    // INVARIANT this depends on — do not break it: nothing writes INIT
                    // back to these statuses except the reducer defaults,
                    // `initializeExistingContent`, and `clearVersions`/
                    // `clearPushPublishHistory`. `loadVersions`/`loadPushPublishHistory`
                    // only ever move LOADING -> LOADED/ERROR, so reading the statuses here
                    // cannot re-trigger them. A loader that reset to INIT on completion or
                    // error would turn this into a fetch loop.
                    //
                    // Nor can the cleared-list fetch be deferred forever: `state: LOADING`
                    // is written only by the two initialize methods, there is no retry that
                    // re-enters it, and the failure path navigates away from the editor
                    // (content.feature.ts) — which tears down this store.
                    const isReloading = store.state() === ComponentStatus.LOADING;
                    const versionsCleared =
                        !isReloading && store.versionsStatus().status === ComponentStatus.INIT;
                    const pushPublishCleared =
                        !isReloading &&
                        store.pushPublishHistoryStatus().status === ComponentStatus.INIT;
                    // Only historical view swaps `contentlet` for an older version. Compare
                    // view leaves `contentlet` live (it only sets `compareContentlet`), so it
                    // must NOT suppress the new-version check — otherwise publishing while
                    // comparing would leave the list stale for the rest of the session.
                    const isViewingHistoricalVersion = store.isViewingHistoricalVersion();

                    untracked(() => {
                        // Only load data if we have a contentlet with an identifier
                        if (!contentlet?.identifier) {
                            return;
                        }

                        const versionsKey = `${contentlet.identifier}:${contentlet.languageId}`;
                        const identityChanged = versionsKey !== loadedVersionsKey;
                        // A workflow action (save/publish/restore) moved the live version
                        // forward. Skipped on the first pass, where there is no baseline yet.
                        //
                        // Also skipped mid-reload, for the same reason the cleared-list
                        // checks are: `initializeExistingContent` resets
                        // `isViewingHistoricalVersion` to false in the SAME patch that flips
                        // state to LOADING, while keeping the outgoing contentlet on screen.
                        // If that contentlet was a historical version, its inode differs from
                        // the live baseline and this would read as a new live version —
                        // firing a request for the identifier we are leaving. The incoming
                        // contentlet triggers the correct load once the reload lands, through
                        // `identityChanged` or the still-INIT `versionsCleared`.
                        const newLiveVersion =
                            !isReloading &&
                            !isViewingHistoricalVersion &&
                            loadedLiveInode !== null &&
                            contentlet.inode !== loadedLiveInode;

                        if (identityChanged || versionsCleared || newLiveVersion) {
                            const isInitialLoad = loadedVersionsKey === null;
                            loadedVersionsKey = versionsKey;

                            if (identityChanged && !isInitialLoad) {
                                // The content identity (locale or identifier) changed under an
                                // active compare/historical session — that state belongs to the
                                // previous context, so discard it. Otherwise a stale
                                // originalContentlet could later restore the previous locale's
                                // content when exiting compare or historical view.
                                // Skipped on a same-identity refetch: there the compare session
                                // is still valid and must survive the reload.
                                patchState(store, {
                                    compareContentlet: null,
                                    historicalVersionInode: null,
                                    originalContentlet: null,
                                    isViewingHistoricalVersion: false
                                });
                            }

                            store.loadVersions({
                                identifier: contentlet.identifier,
                                page: 1
                            });
                        }

                        if (
                            contentlet.identifier !== loadedPushPublishIdentifier ||
                            pushPublishCleared
                        ) {
                            loadedPushPublishIdentifier = contentlet.identifier;
                            store.loadPushPublishHistory({
                                identifier: contentlet.identifier,
                                page: 1
                            });
                        }

                        // Only the live version updates the baseline, so returning from a
                        // historical version lands back on a known inode instead of looking
                        // like a brand-new one. Mid-reload is excluded too: the contentlet on
                        // screen is the outgoing one (possibly a historical version whose
                        // `isViewingHistoricalVersion` flag was already cleared), so adopting
                        // its inode would leave the baseline pointing at a version that was
                        // never the live one.
                        if (!isReloading && !isViewingHistoricalVersion) {
                            loadedLiveInode = contentlet.inode;
                        }
                    });
                });

                /**
                 * Effect that exits compare view when the user leaves the History
                 * sidebar tab — a compare session belongs to that tab's context.
                 */
                let previousSidebarTab: number | null = null;

                effect(() => {
                    const activeSidebarTab = store.uiState().activeSidebarTab;

                    untracked(() => {
                        const tabChanged =
                            previousSidebarTab !== null && activeSidebarTab !== previousSidebarTab;
                        previousSidebarTab = activeSidebarTab;

                        if (
                            tabChanged &&
                            activeSidebarTab !== HISTORY_SIDEBAR_TAB_INDEX &&
                            store.uiState().view === 'compare'
                        ) {
                            store.exitCompareView();
                        }
                    });
                });
            }
        })
    );
}
