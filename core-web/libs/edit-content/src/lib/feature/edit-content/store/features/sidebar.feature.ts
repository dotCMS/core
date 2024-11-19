import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed } from '@angular/core';

import { getPersistSidebarState, setPersistSidebarState } from '../../../../utils/functions.util';
import { ContentState } from './content.feature';

export interface SidebarState {
    showSidebar: boolean;
}

const initialState: SidebarState = {
    showSidebar: false
};

/**
 * Feature that handles the sidebar's state and persistence.
 *
 * @returns {SignalStoreFeature} The feature object.
 */
export function withSidebar() {
    return signalStoreFeature(
        { state: type<ContentState>() },
        withState(initialState),
        withComputed(({ contentlet }) => ({
            getCurrentContentIdentifier: computed(() => contentlet()?.identifier)
        })),
        withMethods((store) => ({
            /**
             * Toggles the visibility of the sidebar by updating the application state
             * and persists the sidebar's state to ensure consistency across sessions.
             */
            toggleSidebar: () => {
                const newSidebarState = !store.showSidebar();
                patchState(store, { showSidebar: newSidebarState });
                setPersistSidebarState(newSidebarState.toString());
            },

            /**
             * Fetches the persistence data from the local storage and updates the application state.
             * Utilizes the `patchState` function to update the global store with the persisted sidebar state.
             */
            getPersistenceDataFromLocalStore: () => {
                patchState(store, { showSidebar: getPersistSidebarState() });
            }
        })),
        withHooks({
            onInit(store) {
                store.getPersistenceDataFromLocalStore();
            }
        })
    );
}
