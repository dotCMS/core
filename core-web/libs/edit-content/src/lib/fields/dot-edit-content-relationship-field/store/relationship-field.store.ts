import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';

import { computed } from '@angular/core';

import { ComponentStatus, DotCMSContentlet } from '@dotcms/dotcms-models';

import { SelectionMode } from '../models/relationship.models';
import { getRelationshipFromContentlet, getSelectionModeByCardinality } from '../utils';

export interface RelationshipFieldState {
    data: DotCMSContentlet[];
    status: ComponentStatus;
    selectionMode: SelectionMode | null;
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
        })
    })),
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
             * Sets the cardinality of the relationship field.
             * @param {number} cardinality - The cardinality of the relationship field.
             */
            initialize(params: {
                cardinality: number;
                contentlet: DotCMSContentlet;
                variable: string;
            }) {
                const { cardinality, contentlet, variable } = params;

                const data = getRelationshipFromContentlet({ contentlet, variable });
                const selectionMode = getSelectionModeByCardinality(cardinality);

                patchState(store, {
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
        };
    })
);
