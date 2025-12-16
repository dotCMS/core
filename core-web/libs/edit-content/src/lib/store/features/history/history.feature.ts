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
import { Router } from '@angular/router';

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
 * Feature store for managing content versions state
 */
export function withHistory() {
    return signalStoreFeature(
        { state: type<EditContentState>() },
        withComputed((store) => ({
            compareData: computed(() => {
                return {
                    inode: store.compareContentlet()?.inode,
                    identifier: store.compareContentlet()?.identifier,
                    language: 'en-us'
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
                router = inject(Router)
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
                                        // Navigate to the restored version if the inode has changed and not in dialog mode
                                        const isDialogMode = store.isDialogMode();
                                        if (
                                            !isDialogMode &&
                                            restoredVersion.inode !== currentContentlet?.inode
                                        ) {
                                            router.navigate(['/content', restoredVersion.inode], {
                                                replaceUrl: true,
                                                queryParamsHandling: 'preserve'
                                            });
                                        }
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
                        icon: 'pi pi-exclamation-triangle text-warning-yellow',
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
                                            isViewingHistoricalVersion: true,
                                            historicalVersionInode: inode
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        // Handle load errors - show error toast and maintain current version
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

                const loadCompareVersionContent = rxMethod<string>(
                    pipe(
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
                                            historicalVersionInode: inode
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        // Handle load errors - show error toast and maintain current version
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
                            case DotHistoryTimelineItemActionType.VIEW:
                                // Determine action based on item type and current store state
                                if (action.item.working) {
                                    // If clicking on working version, exit historical view
                                    exitHistoricalView();
                                } else {
                                    // If clicking on historical version, load it
                                    loadVersionContent(action.item.inode);
                                }
                                break;
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
                                    icon: 'pi pi-exclamation-triangle text-warning-yellow',
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
                 * Effect that automatically loads versions and push publish history when contentlet changes
                 * This ensures both datasets are refreshed when switching between different content
                 */
                effect(() => {
                    const contentlet = store.contentlet();

                    untracked(() => {
                        // Only load data if we have a contentlet with an identifier
                        if (contentlet?.identifier) {
                            // Clear existing data to avoid showing stale data during content switches
                            store.clearVersions();
                            store.clearPushPublishHistory();

                            // Load fresh data for the current contentlet
                            store.loadVersions({
                                identifier: contentlet.identifier,
                                page: 1
                            });

                            store.loadPushPublishHistory({
                                identifier: contentlet.identifier,
                                page: 1
                            });
                        }
                    });
                });
            }
        })
    );
}
