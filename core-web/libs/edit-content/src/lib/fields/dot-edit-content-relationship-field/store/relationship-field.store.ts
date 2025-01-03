import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed } from '@angular/core';

import { tap } from 'rxjs/operators';

import { ComponentStatus } from '@dotcms/dotcms-models';

import { RELATIONSHIP_OPTIONS } from '../dot-edit-content-relationship-field.constants';
import {
    RelationshipFieldItem,
    RelationshipTypes,
    SelectionMode
} from '../models/relationship.models';

export interface RelationshipFieldState {
    data: RelationshipFieldItem[];
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
        totalPages: computed(() => Math.ceil(state.data().length / state.pagination().rowsPerPage)),
        isDisabledCreateNewContent: computed(() => {
            const totalItems = state.data().length;
            const selectionMode = state.selectionMode();

            if (selectionMode === 'single') {
                return totalItems >= 1;
            }

            return false;
        }),
        formattedRelationship: computed(() => {
            const data = state.data();

            return data.map((item) => item.id).join(',');
        })
    })),
    withMethods((store) => {
        return {
            /**
             * Sets the data in the state.
             * @param {RelationshipFieldItem[]} data - The data to be set.
             */
            setData(data: RelationshipFieldItem[]) {
                patchState(store, {
                    data
                });
            },
            /**
             * Sets the cardinality of the relationship field.
             * @param {number} cardinality - The cardinality of the relationship field.
             */
            setCardinality(cardinality: number) {
                const relationshipType = RELATIONSHIP_OPTIONS[cardinality];

                if (!relationshipType) {
                    throw new Error('Invalid relationship type');
                }

                const selectionMode: SelectionMode =
                    relationshipType === RelationshipTypes.ONE_TO_ONE ? 'single' : 'multiple';

                patchState(store, {
                    selectionMode
                });
            },
            /**
             * Adds new data to the existing data in the state.
             * @param {RelationshipFieldItem[]} data - The new data to be added.
             */
            addData(data: RelationshipFieldItem[]) {
                const currentData = store.data();

                const existingIds = new Set(currentData.map((item) => item.id));
                const uniqueNewData = data.filter((item) => !existingIds.has(item.id));
                patchState(store, {
                    data: [...currentData, ...uniqueNewData]
                });
            },
            /**
             * Deletes an item from the store at the specified index.
             * @param index - The index of the item to delete.
             */
            deleteItem(id: string) {
                patchState(store, {
                    data: store.data().filter((item) => item.id !== id)
                });
            },
            /**
             * Loads the data for the relationship field by fetching content from the service.
             * It updates the state with the loaded data and sets the status to LOADED.
             */
            loadData: rxMethod<void>(
                pipe(tap(() => patchState(store, { status: ComponentStatus.LOADED })))
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
                patchState(store, {
                    pagination: {
                        ...store.pagination(),
                        offset: store.pagination().offset - store.pagination().rowsPerPage,
                        currentPage: store.pagination().currentPage - 1
                    }
                });
            }
        };
    }),
    withHooks({
        onInit: (store) => {
            store.loadData();
        }
    })
);
