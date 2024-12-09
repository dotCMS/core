import { tapResponse } from '@ngrx/operators';
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

import { computed, inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { ComponentStatus } from '@dotcms/dotcms-models';

import { RelationshipFieldItem } from '../models/relationship.models';
import { RelationshipFieldService } from '../services/relationship-field.service';

export interface RelationshipFieldState {
    data: RelationshipFieldItem[];
    status: ComponentStatus;
    pagination: {
        offset: number;
        currentPage: number;
        rowsPerPage: number;
    };
}

const initialState: RelationshipFieldState = {
    data: [],
    status: ComponentStatus.INIT,
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
        totalPages: computed(() => Math.ceil(state.data().length / state.pagination().rowsPerPage))
    })),
    withMethods((store) => {
        const relationshipFieldService = inject(RelationshipFieldService);

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
                pipe(
                    tap(() => patchState(store, { status: ComponentStatus.LOADING })),
                    switchMap(() =>
                        relationshipFieldService.getContent(10).pipe(
                            tapResponse({
                                next: (data) =>
                                    patchState(store, { data, status: ComponentStatus.LOADED }),
                                error: () => patchState(store, { status: ComponentStatus.ERROR })
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
