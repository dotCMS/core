import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { tap, switchMap, filter } from 'rxjs/operators';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { Column } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/models/column.model';
import { RelationshipFieldItem } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/models/relationship.models';
import { RelationshipFieldService } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/services/relationship-field.service';

export interface ExistingContentState {
    data: RelationshipFieldItem[];
    status: ComponentStatus;
    errorMessage: string | null;
    columns: Column[];
    pagination: {
        offset: number;
        currentPage: number;
        rowsPerPage: number;
    };
}

const initialState: ExistingContentState = {
    data: [],
    columns: [],
    status: ComponentStatus.INIT,
    errorMessage: null,
    pagination: {
        offset: 0,
        currentPage: 1,
        rowsPerPage: 50
    }
};

/**
 * Store for the ExistingContent component.
 * This store manages the state and actions related to the existing content.
 */
export const ExistingContentStore = signalStore(
    withState(initialState),
    withComputed((state) => ({
        isLoading: computed(() => state.status() === ComponentStatus.LOADING),
        totalPages: computed(() => Math.ceil(state.data().length / state.pagination().rowsPerPage))
    })),
    withMethods((store) => {
        const relationshipFieldService = inject(RelationshipFieldService);

        return {
            /**
             * Initiates the loading of content by setting the status to LOADING and fetching content from the service.
             * @returns {Observable<void>} An observable that completes when the content has been loaded.
             */
            loadContent: rxMethod<string>(
                pipe(
                    tap(() => patchState(store, { status: ComponentStatus.LOADING })),
                    tap((contentTypeId) => {
                        if (!contentTypeId) {
                            patchState(store, {
                                status: ComponentStatus.ERROR,
                                errorMessage: 'dot.file.relationship.dialog.content.id.required'
                            });
                        }
                    }),
                    filter((contentTypeId) => !!contentTypeId),
                    switchMap((contentTypeId) =>
                        relationshipFieldService.getColumnsAndContent(contentTypeId).pipe(
                            tapResponse({
                                next: ([columns, data]) => {
                                    patchState(store, {
                                        columns,
                                        data,
                                        status: ComponentStatus.LOADED
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
             * Applies the initial state for the existing content.
             */
            applyInitialState: () => {
                patchState(store, initialState);
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
