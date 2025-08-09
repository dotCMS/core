import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { catchError, switchMap, tap } from 'rxjs/operators';

import { DotContentTypeService, DotFieldService, DotHttpErrorManagerService } from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSContentlet,
    DotCMSContentType,
    DotCMSContentTypeField,
    FeaturedFlags
} from '@dotcms/dotcms-models';

import { STATIC_COLUMNS } from '../dot-edit-content-relationship-field.constants';
import { SelectionMode, TableColumn } from '../models/relationship.models';
import { getColumns, getContentTypeIdFromRelationship, getRelationshipFromContentlet, getSelectionModeByCardinality } from '../utils';

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
    { providedIn: 'root' },
    withState(initialState),
    withComputed((state) => ({
        /**
         * Computes the total number of pages based on the number of items and the rows per page.
         * @returns {number} The total number of pages.
         */
        totalPages: computed(() => Math.ceil(state.data().length / state.pagination().rowsPerPage)),
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
    })),
    withMethods(
        (
            store,
            dotContentTypeService = inject(DotContentTypeService),
            dotHttpErrorManagerService = inject(DotHttpErrorManagerService),
            dotFieldService = inject(DotFieldService)
        ) => ({
            /**
             * Sets the data in the state.
             * @param {RelationshipFieldItem[]} data - The data to be set.
             */
            setData(data: DotCMSContentlet[]) {
                patchState(store, { data: [...data] });
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
                    switchMap(({ field, contentlet }) => {
                        const cardinality = field?.relationships?.cardinality ?? null;
                        const contentTypeId = getContentTypeIdFromRelationship(field);

                        if (cardinality === null || !field?.variable || !contentTypeId) {
                            return EMPTY;
                        }

                        // Validate and set initial data
                        const data = getRelationshipFromContentlet({ contentlet, variable: field.variable });
                        const selectionMode = getSelectionModeByCardinality(cardinality);
                        const columns = getColumns(field, data);

                        // Reset state first
                        patchState(store, {
                            status: ComponentStatus.LOADING,
                            contentType: null, // Reset contentType to prevent stale state
                            isNewEditorEnabled: false, // Reset isNewEditorEnabled to prevent stale state
                            selectionMode,
                            columns,
                            data,
                            field
                        });

                        // Continue with content type loading
                        return dotContentTypeService.getContentType(contentTypeId).pipe(
                            tap((contentType) => {
                                const isNewEditorEnabled =
                                    contentType.metadata?.[
                                        FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED
                                    ] === true;

                                patchState(store, {
                                    contentType,
                                    isNewEditorEnabled,
                                    status: ComponentStatus.LOADED
                                });
                            })
                        );
                    }),
                    catchError((error) => {
                        // Handle all errors (validation and async) here
                        dotHttpErrorManagerService.handle(error);
                        patchState(store, {
                            status: ComponentStatus.ERROR
                        });

                        return EMPTY;
                    })
                )
            ),
            /**
             * Deletes an item from the store at the specified index.
             * @param index - The index of the item to delete.
             */
            deleteItem(inode: string) {
                patchState(store, {
                    data: store.data().filter((item) => item.inode !== inode)
                });
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
