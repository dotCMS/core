import { faker } from '@faker-js/faker';
import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed } from '@angular/core';

import { ComponentStatus } from '@dotcms/dotcms-models';

export interface Content {
    id: string;
    title: string;
    step: string;
    description: string;
    lastUpdate: string;
}

export interface ExistingContentState {
    data: Content[];
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
            const mockData = Array.from({ length: 100 }, () => ({
                id: faker.string.uuid(),
                title: faker.lorem.sentence(),
                step: faker.helpers.arrayElement(['Draft', 'Published', 'Archived']),
                description: faker.lorem.paragraph(),
                lastUpdate: faker.date.recent().toISOString()
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
