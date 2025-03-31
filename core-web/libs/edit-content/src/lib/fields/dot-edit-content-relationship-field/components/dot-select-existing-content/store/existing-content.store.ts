import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { TablePageEvent } from 'primeng/table';

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
    searchData: DotCMSContentlet[];
    selectionMode: SelectionMode | null;
    errorMessage: string | null;
    columns: Column[];
    pagination: {
        offset: number;
        currentPage: number;
        rowsPerPage: number;
        totalResults: number;
    };
    selectedItemsIds: string[];
    showOnlySelected: boolean;
}

const paginationInitialState: ExistingContentState['pagination'] = {
    offset: 0,
    currentPage: 1,
    rowsPerPage: 50,
    totalResults: 0
};

const initialState: ExistingContentState = {
    contentTypeId: null,
    data: [],
    searchData: [],
    columns: [],
    status: ComponentStatus.INIT,
    selectionMode: null,
    errorMessage: null,
    pagination: { ...paginationInitialState },
    selectedItemsIds: [],
    showOnlySelected: false
};

/**
 * Store for the ExistingContent component.
 * This store manages the state and actions related to the existing content.
 */
export const ExistingContentStore = signalStore(
    { providedIn: 'root' },
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
         * Computes the items based on the selected items.
         * @returns {DotCMSContentlet[]} The items.
         */
        currentItems: computed(() => {
            const showOnlySelected = state.showOnlySelected();
            const selectedItemsIds = state.selectedItemsIds();
            const data = showOnlySelected ? state.searchData() : state.data();

            return data.filter((item) => selectedItemsIds.includes(item.inode));
        }),
        /**
         * Computes the filtered data based on the showOnlySelected state.
         * @returns {DotCMSContentlet[]} The filtered data.
         */
        filteredData: computed(() => {
            const showOnlySelected = state.showOnlySelected();
            const selectedItemsIds = state.selectedItemsIds();
            const data = showOnlySelected ? state.data() : state.searchData();

            if (showOnlySelected && selectedItemsIds.length > 0) {
                return data.filter((item) => selectedItemsIds.includes(item.inode));
            }

            return data;
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
                selectedItemsIds: string[];
            }>(
                pipe(
                    tap(({ selectionMode }) =>
                        patchState(store, {
                            status: ComponentStatus.LOADING,
                            selectionMode,
                            pagination: { ...paginationInitialState }
                        })
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
                    switchMap(({ contentTypeId, selectedItemsIds }) =>
                        relationshipFieldService.getColumnsAndContent(contentTypeId).pipe(
                            tapResponse({
                                next: ([columns, searchResponse]) => {
                                    patchState(store, {
                                        contentTypeId,
                                        columns,
                                        data: searchResponse.contentlets,
                                        searchData: searchResponse.contentlets,
                                        status: ComponentStatus.LOADED,
                                        selectedItemsIds,
                                        pagination: {
                                            ...store.pagination(),
                                            totalResults: searchResponse.totalResults
                                        }
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
                const { currentPage } = store.pagination();

                if (currentPage === 1) {
                    return;
                }

                patchState(store, {
                    pagination: {
                        ...store.pagination(),
                        offset: store.pagination().offset - store.pagination().rowsPerPage,
                        currentPage: currentPage - 1
                    }
                });
            },
            /**
             * Sets the selected items in the state.
             * @param items The items to set as selected.
             */
            setSelectedItems: (items: DotCMSContentlet[]) => {
                const selectedInodes = items.map((item) => item.inode);
                patchState(store, {
                    selectedItemsIds: selectedInodes
                });
            },
            /**
             * Toggles between showing all items or only selected items.
             */
            toggleShowOnlySelected: () => {
                patchState(store, {
                    showOnlySelected: !store.showOnlySelected()
                });
            },
            /**
             * Sets the offset and current page in the state.
             * @param event The event containing the first and rows properties.
             */
            setOffset: ({ first }: TablePageEvent) => {
                patchState(store, {
                    pagination: {
                        ...store.pagination(),
                        offset: first,
                        currentPage: Math.floor(first / store.pagination().rowsPerPage) + 1
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
                            pagination: { ...paginationInitialState }
                        })
                    ),
                    switchMap((searchParams) => {

                        // Map SearchParams to RelationshipFieldQueryParams
                        const queryParams: RelationshipFieldQueryParams = {
                            contentTypeId: store.contentTypeId()
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
                                        searchData: data.contentlets,
                                        status: ComponentStatus.LOADED,
                                        pagination: {
                                            ...store.pagination(),
                                            totalResults: data.totalResults
                                        }
                                    });
                                },
                                error: () => {
                                    patchState(store, {
                                        status: ComponentStatus.ERROR,
                                        errorMessage: 'dot.file.relationship.dialog.search.failed'
                                    });
                                }
                            })
                        );
                    })
                )
            )
        };
    })
);
