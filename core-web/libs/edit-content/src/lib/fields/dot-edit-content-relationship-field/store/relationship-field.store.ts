import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';

import { ComponentStatus } from '@dotcms/dotcms-models';

export interface RelationshipFieldItem {
    id: string;
    title: string;
    language: string;
    state: string;
}

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
        rowsPerPage: 10
    }
};

/**
 * Store for the RelationshipField component.
 * This store manages the state and actions related to the relationship field.
 */
export const RelationshipFieldStore = signalStore(
    withState(initialState),
    withMethods((store) => ({
        setData(data: RelationshipFieldItem[]) {
            patchState(store, {
                data
            });
        }
    }))
);
