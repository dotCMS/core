
import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed } from '@angular/core';

import { ComponentStatus, DotCMSContentlet } from '@dotcms/dotcms-models';
import { URL_MAP_CONTENTLET } from '@dotcms/utils-testing';

export interface ExistingContentState {
    data: DotCMSContentlet[];
    status: ComponentStatus;
    pagination: {
        offset: number;
        currentPage: number;
        rowsPerPage: number;
    };
}

const initialState: ExistingContentState = {
    data: [],
    status: ComponentStatus.INIT,
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
    withMethods((store) => ({
        loadContent() {
            const mockData = Array.from({ length: 100 }, (_, index) => ({
                ...URL_MAP_CONTENTLET,
                identifier: `${index}`
            }));
            patchState(store, {
                data: mockData
            });
        },
        nextPage() {
            patchState(store, {
                pagination: {
                    ...store.pagination(),
                    offset: store.pagination().offset + store.pagination().rowsPerPage,
                    currentPage: store.pagination().currentPage + 1
                }
            });
        },
        previousPage() {
            patchState(store, {
                pagination: {
                    ...store.pagination(),
                    offset: store.pagination().offset - store.pagination().rowsPerPage,
                    currentPage: store.pagination().currentPage - 1
                }
            });
        }
    })),
    withHooks({
        onInit: (store) => {
            store.loadContent();
        }
    })
);
