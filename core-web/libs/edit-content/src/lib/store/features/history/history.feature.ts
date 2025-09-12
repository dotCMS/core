import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';

import { ConfirmationService, MessageService } from 'primeng/api';

import { switchMap, tap } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotVersionableService,
    DotMessageService
} from '@dotcms/data-access';
import { ComponentStatus, DotCMSContentletVersion, DotPagination } from '@dotcms/dotcms-models';

import { ContentletIdentifier } from '../../../models/dot-edit-content-field.type';
import {
    DotHistoryTimelineItemAction,
    DotHistoryTimelineItemActionType
} from '../../../models/dot-edit-content.model';
import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { EditContentState } from '../../edit-content.store';

/**
 * Default number of items per page for versions pagination. Need 40 so scroll works correctly in large viewports.
 */
const DEFAULT_VERSIONS_PER_PAGE = 40;

/**
 * Feature store for managing content versions state
 */
export function withHistory() {
    return signalStoreFeature(
        { state: type<EditContentState>() },
        withMethods(
            (
                store,
                dotEditContentService = inject(DotEditContentService),
                errorManager = inject(DotHttpErrorManagerService),
                confirmationService = inject(ConfirmationService),
                dotVersionableService = inject(DotVersionableService),
                messageService = inject(MessageService),
                dotMessageService = inject(DotMessageService)
            ) => {
                /**
                 * Deletes a version by inode and reloads the versions list reactively
                 * @param inode - The inode of the version to delete
                 */
                const deleteVersionMethod = rxMethod<string>(
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
                                            { offset: 1, limit: DEFAULT_VERSIONS_PER_PAGE }
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
                                const limit =
                                    currentPagination?.perPage || DEFAULT_VERSIONS_PER_PAGE;

                                // Detect if we're switching content or starting fresh
                                const isNewContent =
                                    currentPagination === null || currentVersions.length === 0;

                                return dotEditContentService
                                    .getVersions(identifier, { offset: page, limit })
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
                     * Resets versions to empty array
                     * Useful when switching content or starting fresh
                     */
                    resetVersions: () => {
                        patchState(store, {
                            versions: []
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
                     * Exposes the delete version method for external use
                     */
                    deleteVersion: deleteVersionMethod,

                    /**
                     * Handles history timeline item actions
                     * Centralizes all version-related actions for reusability across components
                     *
                     * @param action - The action object containing type and item data
                     */
                    handleHistoryAction: (action: DotHistoryTimelineItemAction) => {
                        switch (action.type) {
                            case DotHistoryTimelineItemActionType.PREVIEW:
                                // TODO: Implement preview functionality

                                break;
                            case DotHistoryTimelineItemActionType.RESTORE:
                                // TODO: Implement restore functionality

                                break;
                            case DotHistoryTimelineItemActionType.COMPARE:
                                // TODO: Implement compare functionality

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
                                        deleteVersionMethod(action.item.inode);
                                    }
                                });
                                break;
                        }
                    }
                };
            }
        )
    );
}
