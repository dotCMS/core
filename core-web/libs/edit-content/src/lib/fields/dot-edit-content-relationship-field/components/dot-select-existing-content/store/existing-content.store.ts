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
}

const initialState: ExistingContentState = {
    data: [],
    status: ComponentStatus.INIT
};

export const ExistingContentStore = signalStore(
    withState(initialState),
    withComputed((state) => ({
        isLoading: computed(() => state.status() === ComponentStatus.LOADING)
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
        }
    })),
    withHooks({
        onInit: (store) => {
            store.loadContent();
        }
    })
);
