import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';

import { computed } from '@angular/core';

export interface RelationshipFieldItem {
    id: string;
    title: string;
    language: string;
    state: string;
}

export interface RelationshipFieldState {
    data: RelationshipFieldItem[];
}

const initialState: RelationshipFieldState = {
    data: []
};

export const RelationshipFieldStore = signalStore(
    withState(initialState),
    withComputed(({ data }) => ({
        data2: computed(() => data())
    })),
    withMethods((store) => ({
        setData(data: RelationshipFieldItem[]) {
            patchState(store, {
                data
            });
        }
    }))
);
