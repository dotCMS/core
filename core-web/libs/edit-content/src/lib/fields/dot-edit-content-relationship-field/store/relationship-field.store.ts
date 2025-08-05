import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY } from 'rxjs';

import { computed, inject } from '@angular/core';


import { catchError, switchMap, tap } from 'rxjs/operators';

import { DotContentTypeService, DotHttpErrorManagerService } from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSContentlet,
    DotCMSContentType,
    FeaturedFlags,
  DotCMSContentTypeField
} from '@dotcms/dotcms-models';


import { ACTION_COLUMN, DEFAULT_RELATIONSHIP_COLUMNS, REORDER_COLUMN } from '../dot-edit-content-relationship-field.constants';
import { SelectionMode, TableColumn } from '../models/relationship.models';
import { createColumn, extractShowFields, getFieldHeader, getFieldWidth, getRelationshipFromContentlet, getSelectionModeByCardinality } from '../utils';

export interface RelationshipFieldState {
    data: DotCMSContentlet[];
    status: ComponentStatus;
    selectionMode: SelectionMode | null;
    field: DotCMSContentTypeField | null;
    pagination: {
        offset: number;
        currentPage: number;
        rowsPerPage: number;
    };
}

const initialState: RelationshipFieldState = {
    data: [],
    status: ComponentStatus.INIT,
    selectionMode: null,
    field: null,
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
    withComputed((state) => {
        return {
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

            /**
             * A computed signal that extracts the showFields variable from field variables.
             * This determines which columns should be displayed in the relationship table.
             */
            showFields: computed(() => extractShowFields(state.field())),

            /**
             * A computed signal that defines the table columns structure.
             * Dynamically builds columns based on showFields() content.
             */
            columns: computed<TableColumn[]>(() => {
                const isEmpty = state.data().length === 0;
                const field = state.field();
                const showFields = extractShowFields(field);

                // Fixed columns that always appear
                const columns: TableColumn[] = [
                    REORDER_COLUMN
                ];

                // Dynamic center columns
                if (showFields && showFields.length > 0) {
                    // Use custom fields from showFields
                    showFields.forEach(fieldName => {
                        columns.push(createColumn(
                            fieldName,
                            getFieldHeader(fieldName),
                            getFieldWidth(fieldName)
                        ));
                    });
                } else {
                    // Use default columns when no showFields
                    columns.push(
                        { ...DEFAULT_RELATIONSHIP_COLUMNS.TITLE, width: isEmpty ? undefined : '12rem' },
                        { ...DEFAULT_RELATIONSHIP_COLUMNS.LANGUAGE, width: isEmpty ? undefined : '8rem' },
                        { ...DEFAULT_RELATIONSHIP_COLUMNS.STATUS, width: isEmpty ? undefined : '8rem' }
                    );
                }

                // Fixed actions column always at the end
                columns.push(ACTION_COLUMN);

                return columns;
            })
        };
    }),
    withMethods((store) => {
        return {
            /**
             * Sets the data in the state.
             * @param {RelationshipFieldItem[]} data - The data to be set.
             */
            setData(data: DotCMSContentlet[]) {
                patchState(store, { data: [...data] });
            },
            /**
             * Initializes the relationship field store with field data and relationship settings.
             * @param {Object} params - The initialization parameters.
             * @param {DotCMSContentTypeField} params.field - The complete field configuration.
             * @param {DotCMSContentlet} params.contentlet - The contentlet data.
             */
            initialize(params: {
                field: DotCMSContentTypeField;
                contentlet: DotCMSContentlet;
            }) {
                const { field, contentlet } = params;
                const cardinality = field?.relationships?.cardinality;

                if (cardinality == null || !field?.variable) {
                    return;
                }

                const data = getRelationshipFromContentlet({ contentlet, variable: field.variable });
                const selectionMode = getSelectionModeByCardinality(cardinality);

                patchState(store, {
                    field,
                    selectionMode,
                    data
                });
            },
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


