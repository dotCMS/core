import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { tap, switchMap, filter } from 'rxjs/operators';

import { ComponentStatus, DotCMSContentlet } from '@dotcms/dotcms-models';
import { Column } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/models/column.model';
import { SelectionMode } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/models/relationship.models';
import { SearchParams } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/models/search.model';
import {
    RelationshipFieldService,
    RelationshipFieldQueryParams
} from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/services/relationship-field.service';

export interface ExistingContentState {
    contentTypeId: string;
    data: DotCMSContentlet[];
    status: ComponentStatus;
    selectionMode: SelectionMode | null;
    errorMessage: string | null;
    columns: Column[];
    currentItemsIds: string[];
    pagination: {
        offset: number;
        currentPage: number;
        rowsPerPage: number;
    };
}

const initialState: ExistingContentState = {
    contentTypeId: null,
    data: [],
    columns: [],
    status: ComponentStatus.INIT,
    selectionMode: null,
    errorMessage: null,
    pagination: {
        offset: 0,
        currentPage: 1,
        rowsPerPage: 50
    },
    currentItemsIds: []
};

/**
 * Store for the ExistingContent component.
 * This store manages the state and actions related to the existing content.
 */
export const ExistingContentStore = signalStore(
    withState(initialState),
    withComputed((state) => ({
        /**
         * Computes whether the content is currently loading.
         * @returns {boolean} True if the content is loading, false otherwise.
         */
        isLoading: computed(() => state.status() === ComponentStatus.LOADING),
        /**
         * Computes the total number of pages based on the data and rows per page.
         * @returns {number} The total number of pages.
         */
        totalPages: computed(() => Math.ceil(state.data().length / state.pagination().rowsPerPage)),
        /**
         * Computes the selected items based on the current items IDs.
         * @returns {DotCMSContentlet[]} The selected items.
         */
        selectedItems: computed(() => {
            const data = state.data();
            const currentItemsIds = state.currentItemsIds();

            return data.filter((item) => currentItemsIds.includes(item.inode));
        })
    })),
    withMethods((store) => {
        const relationshipFieldService = inject(RelationshipFieldService);

        return {
            /**
             * Initiates the loading of content by setting the status to LOADING and fetching content from the service.
             * @returns {Observable<void>} An observable that completes when the content has been loaded.
             */
            initLoad: rxMethod<{
                contentTypeId: string;
                selectionMode: SelectionMode;
                currentItemsIds: string[];
            }>(
                pipe(
                    tap(({ selectionMode }) =>
                        patchState(store, { status: ComponentStatus.LOADING, selectionMode })
                    ),
                    tap(({ contentTypeId }) => {
                        if (!contentTypeId) {
                            patchState(store, {
                                status: ComponentStatus.ERROR,
                                errorMessage: 'dot.file.relationship.dialog.content.id.required'
                            });
                        }
                    }),
                    filter(({ contentTypeId }) => !!contentTypeId),
                    switchMap(({ contentTypeId, currentItemsIds }) =>
                        relationshipFieldService.getColumnsAndContent(contentTypeId).pipe(
                            tapResponse({
                                next: ([columns, data]) => {
                                    patchState(store, {
                                        contentTypeId,
                                        columns,
                                        data,
                                        status: ComponentStatus.LOADED,
                                        currentItemsIds
                                    });
                                },
                                error: () =>
                                    patchState(store, {
                                        status: ComponentStatus.ERROR,
                                        errorMessage:
                                            'dot.file.relationship.dialog.content.request.failed'
                                    })
                            })
                        )
                    )
                )
            ),
            /**
             * Advances the pagination to the next page and updates the state accordingly.
             */
            nextPage: () => {
                patchState(store, {
                    pagination: {
                        ...store.pagination(),
                        offset: store.pagination().offset + store.pagination().rowsPerPage,
                        currentPage: store.pagination().currentPage + 1
                    }
                });
            },
            /**
             * Moves the pagination to the previous page and updates the state accordingly.
             */
            previousPage: () => {
                const { currentPage, offset, rowsPerPage } = store.pagination();

                if (currentPage === 1) {
                    return;
                }

                patchState(store, {
                    pagination: {
                        ...store.pagination(),
                        offset: offset - rowsPerPage,
                        currentPage: currentPage - 1
                    }
                });
            },
            /**
             * Searches for content based on the provided search parameters.
             * @param searchParams The search parameters to use for filtering content.
             * @returns An observable that completes when the search has been performed.
             */
            search: rxMethod<SearchParams>(
                pipe(
                    tap(() =>
                        patchState(store, {
                            status: ComponentStatus.LOADING,
                            // Reset pagination when performing a new search
                            pagination: {
                                ...store.pagination(),
                                offset: 0,
                                currentPage: 1
                            }
                        })
                    ),
                    switchMap((searchParams) => {
                        // Map SearchParams to RelationshipFieldQueryParams
                        const queryParams: RelationshipFieldQueryParams = {
                            contentTypeId: store.contentTypeId(),
                            page: store.pagination().currentPage,
                            perPage: store.pagination().rowsPerPage
                        };

                        if (searchParams.query) {
                            queryParams.globalSearch = searchParams.query;
                        }

                        if (
                            searchParams.systemSearchableFields &&
                            Object.keys(searchParams.systemSearchableFields).length > 0
                        ) {
                            queryParams.systemSearchableFields = {
                                ...searchParams.systemSearchableFields
                            };
                        }

                        return relationshipFieldService.search(queryParams).pipe(
                            tapResponse({
                                next: (data) => {
                                    patchState(store, {
                                        data,
                                        status: ComponentStatus.LOADED
                                    });
                                },
                                error: (error) => {
                                    patchState(store, {
                                        status: ComponentStatus.ERROR,
                                        errorMessage: 'dot.file.relationship.dialog.search.failed'
                                    });
                                    console.error('Search failed:', error);
                                }
                            })
                        );
                    })
                )
            )
        };
    })
);
