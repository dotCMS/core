import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, type, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { ComponentStatus, DotCMSContentletVersion } from '@dotcms/dotcms-models';

import { ContentletIdentifier } from '../../../models/dot-edit-content-field.type';
import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { EditContentState } from '../../edit-content.store';

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
                errorManager = inject(DotHttpErrorManagerService)
            ) => ({
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
                            const limit = currentPagination?.perPage || 20;

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
                                                versionsPagination: response.pagination,
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
                }
            })
        )
    );
}
