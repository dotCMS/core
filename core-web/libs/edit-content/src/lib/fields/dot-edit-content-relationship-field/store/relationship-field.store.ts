import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSContentlet,
    DotCMSContentType,
    DotCMSContentTypeField
} from '@dotcms/dotcms-models';

import { RelationshipFieldService } from './relationship-field.service';

import { STATIC_COLUMNS } from '../dot-edit-content-relationship-field.constants';
import { SelectionMode, TableColumn } from '../models/relationship.models';

export interface RelationshipFieldState {
    data: DotCMSContentlet[];
    status: ComponentStatus;
    field: DotCMSContentTypeField | null;
    selectionMode: SelectionMode | null;
    contentType: DotCMSContentType | null;
    isNewEditorEnabled: boolean;
    staticColumns: number;
    columns: TableColumn[];
    pagination: {
        offset: number;
        currentPage: number;
        rowsPerPage: number;
    };
}

const initialState: RelationshipFieldState = {
    data: [],
    status: ComponentStatus.INIT,
    field: null,
    columns: [],
    selectionMode: null,
    contentType: null,
    isNewEditorEnabled: false,
    staticColumns: STATIC_COLUMNS,
    pagination: {
        offset: 0,
        currentPage: 1,
        rowsPerPage: 6
    }
};

/**
 * Store for the RelationshipField component.
 * This store manages the state and actions related to the relationship field.
 */
export const RelationshipFieldStore = signalStore(
    withState(initialState),
    withComputed((state) => ({
        /**
         * Computes the total number of pages based on the number of items and the rows per page.
         * @returns {number} The total number of pages.
         */
        totalPages: computed(() => Math.ceil(state.data().length / state.pagination().rowsPerPage)),
        /**
         * Returns the slice of data for the current page based on offset and rowsPerPage.
         * @returns {DotCMSContentlet[]} The items for the current page.
         */
        paginatedData: computed(() => {
            const allData = state.data();
            const { offset, rowsPerPage } = state.pagination();

            return allData.slice(offset, offset + rowsPerPage);
        }),
        /**
         * Checks if the create new content button is disabled based on the selection mode and the number of items.
         * @returns {boolean} True if the button is disabled, false otherwise.
         */
        isDisabledCreateNewContent: computed(() => {
            const totalItems = state.data().length;
            const selectionMode = state.selectionMode();

            if (selectionMode === 'single') {
                return totalItems >= 1;
            }

            return false;
        }),
        /**
         * Formats the relationship field data into a string of IDs.
         * @returns {string} A string of IDs separated by commas.
         */
        formattedRelationship: computed(() => {
            const data = state.data();
            const identifiers = data.map((item) => item.identifier).join(',');

            return `${identifiers}`;
        }),
        showThumbnail: computed(() =>
            state
                .data()
                .some(
                    (item) =>
                        item.hasTitleImage === true || (item.hasTitleImage as unknown) === 'true'
                )
        )
    })),
    withMethods(
        (
            store,
            relationshipFieldService = inject(RelationshipFieldService),
            dotHttpErrorManagerService = inject(DotHttpErrorManagerService)
        ) => ({
            /**
             * Sets the data in the state.
             * @param {RelationshipFieldItem[]} data - The data to be set.
             */
            setData(data: DotCMSContentlet[]) {
                patchState(store, {
                    data: [...data],
                    pagination: { ...store.pagination(), offset: 0, currentPage: 1 }
                });
            },
            /**
             * Initializes the relationship field with the provided parameters.
             * @param {object} params - The initialization parameters.
             * @param {number} params.cardinality - The cardinality of the relationship field.
             * @param {DotCMSContentlet} params.contentlet - The contentlet containing the relationship data.
             * @param {string} params.variable - The variable name for the relationship field.
             * @param {string} params.contentTypeId - The ID of the content type to load.
             */
            initialize: rxMethod<{
                field: DotCMSContentTypeField;
                contentlet: DotCMSContentlet;
            }>(
                pipe(
                    tap(() => patchState(store, initialState)),
                    switchMap(({ field, contentlet }) => {
                        return relationshipFieldService.prepareField({ field, contentlet }).pipe(
                            tapResponse({
                                next: (newState) => {
                                    patchState(store, {
                                        status: ComponentStatus.LOADED,
                                        contentType: newState.contentType,
                                        isNewEditorEnabled: newState.isNewEditorEnabled,
                                        selectionMode: newState.selectionMode,
                                        columns: newState.columns,
                                        data: newState.data,
                                        field
                                    });
                                },
                                error: (error) => {
                                    if (error instanceof HttpErrorResponse) {
                                        dotHttpErrorManagerService.handle(error);
                                    }
                                    patchState(store, {
                                        status: ComponentStatus.ERROR
                                    });
                                }
                            })
                        );
                    })
                )
            ),
            /**
             * Deletes an item from the store by inode.
             * If the current page offset exceeds the new data length, pagination resets to the last valid page.
             * @param inode - The inode of the item to delete.
             */
            deleteItem(inode: string) {
                const newData = store.data().filter((item) => item.inode !== inode);
                const { offset, rowsPerPage } = store.pagination();

                if (offset >= newData.length && newData.length > 0) {
                    const lastPage = Math.ceil(newData.length / rowsPerPage);
                    const newOffset = (lastPage - 1) * rowsPerPage;
                    patchState(store, {
                        data: newData,
                        pagination: {
                            ...store.pagination(),
                            offset: newOffset,
                            currentPage: lastPage
                        }
                    });
                } else if (newData.length === 0) {
                    patchState(store, {
                        data: newData,
                        pagination: {
                            ...store.pagination(),
                            offset: 0,
                            currentPage: 1
                        }
                    });
                } else {
                    patchState(store, { data: newData });
                }
            },
            /**
             * Reorders the data without resetting the current pagination.
             * Used after drag-and-drop row reorder to preserve the current page.
             * @param {DotCMSContentlet[]} data - The reordered data array.
             */
            reorderData(data: DotCMSContentlet[]) {
                patchState(store, { data: [...data] });
            },
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
                patchState(store, {
                    pagination: {
                        ...store.pagination(),
                        offset: store.pagination().offset - store.pagination().rowsPerPage,
                        currentPage: store.pagination().currentPage - 1
                    }
                });
            }
        })
    )
);
