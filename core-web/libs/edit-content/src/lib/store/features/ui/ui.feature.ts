import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withHooks,
    withMethods
} from '@ngrx/signals';

import { computed, effect, untracked } from '@angular/core';

import { getStoredUIState, saveStoreUIState } from '../../../utils/functions.util';
import { EditContentState } from '../../edit-content.store';

/**
 * Feature that manages UI-related state for the content editor
 * This includes tab management and potentially other UI-specific state
 */
export function withUI() {
    return signalStoreFeature(
        { state: type<EditContentState>() },

        withComputed((store) => ({
            /**
             * Computed property that returns the currently active tab index.
             * Returns 0 if the initial content state is 'new'.
             */
            activeTab: computed(() => {
                const initialState = store.initialContentletState();
                const uiState = store.uiState();

                return initialState === 'new' ? 0 : uiState.activeTab;
            }),
            /**
             * Computed property that returns the sidebar visibility state
             */
            isSidebarOpen: computed(() => store.uiState().isSidebarOpen),
            /**
             * Computed property that returns the active sidebar tab
             */
            activeSidebarTab: computed(() => {
                const initialState = store.initialContentletState();
                const uiState = store.uiState();

                return initialState === 'new' ? 0 : uiState.activeSidebarTab;
            }),

            /**
             * Computed property that returns the beta message visibility state
             */
            isBetaMessageVisible: computed(() => store.uiState().isBetaMessageVisible)
        })),
        withMethods((store) => ({
            /**
             * Sets the active tab index in the store and persists it to localStorage
             * @param index - The index of the tab to set as active
             */
            setActiveTab(index: number): void {
                const newState = {
                    ...store.uiState(),
                    activeTab: index
                };
                patchState(store, { uiState: newState });
            },

            /**
             * Toggles the sidebar visibility and persists it to localStorage
             */
            toggleSidebar(): void {
                const newState = {
                    ...store.uiState(),
                    isSidebarOpen: !store.uiState().isSidebarOpen
                };
                patchState(store, { uiState: newState });
            },

            /**
             * Toggles the beta message visibility and persists it to localStorage
             */
            toggleBetaMessage(): void {
                const newState = {
                    ...store.uiState(),
                    isBetaMessageVisible: !store.uiState().isBetaMessageVisible
                };
                patchState(store, { uiState: newState });
            },

            /**
             * Sets the active sidebar tab and persists it to localStorage
             * @param tab - The tab to set as active
             */
            setActiveSidebarTab(tab: number): void {
                const newState = {
                    ...store.uiState(),
                    activeSidebarTab: tab
                };
                patchState(store, { uiState: newState });
            },

            /**
             * Enables dialog mode to bypass route-based initialization
             * This should be called when the store is used from DotCreateContentDialogComponent
             */
            enableDialogMode(): void {
                patchState(store, { isDialogMode: true });
            },

            /**
             * Disables dialog mode to enable route-based initialization
             */
            disableDialogMode(): void {
                patchState(store, { isDialogMode: false });
            }
        })),
        withHooks({
            onInit(store) {
                const storedState = getStoredUIState();
                patchState(store, { uiState: storedState });

                effect(() => {
                    const uiState = store.uiState();
                    untracked(() => {
                        saveStoreUIState(uiState);
                    });
                });
            }
        })
    );
}
