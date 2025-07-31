/* eslint-disable @typescript-eslint/no-explicit-any */
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { signalStore, signalStoreFeature, withState } from '@ngrx/signals';

import { fakeAsync } from '@angular/core/testing';

import { withUI } from './ui.feature';

import { DotContentletState } from '../../../models/dot-edit-content.model';
import { getStoredUIState, saveStoreUIState } from '../../../utils/functions.util';
import { initialRootState } from '../../edit-content.store';

jest.mock('../../../utils/functions.util', () => ({
    getStoredUIState: jest.fn(() => ({
        activeTab: 0,
        isSidebarOpen: true,
        activeSidebarTab: 0
    })),
    saveStoreUIState: jest.fn()
}));

describe('UIFeature', () => {
    let spectator: SpectatorService<any>;
    let store: any;

    const withTest = () =>
        signalStoreFeature(
            withState({
                ...initialRootState,
                initialContentletState: 'edit' as DotContentletState
            })
        );

    const createStore = createServiceFactory({
        service: signalStore(withTest(), withUI())
    });

    beforeEach(() => {
        jest.clearAllMocks();
        spectator = createStore();
        store = spectator.service;
    });

    describe('Store Initialization', () => {
        it('should load initial state from localStorage', () => {
            expect(getStoredUIState).toHaveBeenCalled();
            expect(store.uiState()).toEqual({
                activeTab: 0,
                isSidebarOpen: true,
                activeSidebarTab: 0
            });
        });

        it('should save state changes to localStorage via effect', fakeAsync(() => {
            // Initial save from initialization
            spectator.flushEffects();
            expect(saveStoreUIState).toHaveBeenCalledWith(store.uiState());

            // Clear mock to test next state change
            jest.clearAllMocks();

            // Make a state change
            store.setActiveTab(2);
            spectator.flushEffects();

            // Verify effect triggered save
            expect(saveStoreUIState).toHaveBeenCalledWith({
                ...store.uiState(),
                activeTab: 2
            });
        }));
    });

    describe('Computed Properties', () => {
        it('should compute activeTab based on initialContentletState', () => {
            expect(store.activeTab()).toBe(0);

            store.setActiveTab(2);
            expect(store.activeTab()).toBe(2);
        });

        it('should compute isSidebarOpen', () => {
            expect(store.isSidebarOpen()).toBe(true);
        });

        it('should compute activeSidebarTab', () => {
            expect(store.activeSidebarTab()).toBe(0);
        });
    });

    describe('Methods', () => {
        describe('setActiveTab', () => {
            it('should update active tab in state', () => {
                store.setActiveTab(2);
                expect(store.activeTab()).toBe(2);
            });
        });

        describe('toggleSidebar', () => {
            it('should toggle sidebar visibility in state', () => {
                const initialState = store.isSidebarOpen();
                store.toggleSidebar();
                expect(store.isSidebarOpen()).toBe(!initialState);
            });

            it('should toggle back to original state when called twice', () => {
                const initialState = store.isSidebarOpen();
                store.toggleSidebar();
                store.toggleSidebar();
                expect(store.isSidebarOpen()).toBe(initialState);
            });
        });

        describe('setActiveSidebarTab', () => {
            it('should update active sidebar tab in state', () => {
                store.setActiveSidebarTab(1);
                expect(store.activeSidebarTab()).toBe(1);
            });
        });
    });
});
