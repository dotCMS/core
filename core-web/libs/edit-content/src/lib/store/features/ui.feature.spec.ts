jest.mock('../../utils/functions.util', () => ({
    getStoredUIState: jest.fn(() => ({
        activeTab: 0,
        isSidebarOpen: true,
        activeSidebarTab: 0
    })),
    saveStoreUIState: jest.fn()
}));

import { signalStore, withState } from '@ngrx/signals';

import { uiInitialState, withUI } from './ui.feature';

import { saveStoreUIState } from '../../utils/functions.util';
import { initialRootState } from '../edit-content.store';

describe('UI Feature', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    const createTestStore = () => {
        const TestStore = signalStore(withState(initialRootState), withUI());

        return new TestStore();
    };

    let store: ReturnType<typeof createTestStore>;

    beforeEach(() => {
        store = createTestStore();
    });

    describe('Computed Properties', () => {
        it('should compute activeTab', () => {
            expect(store.activeTab()).toBe(0);
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
            it('should update active tab and persist to localStorage', () => {
                store.setActiveTab(2);

                expect(store.activeTab()).toBe(2);
                expect(saveStoreUIState).toHaveBeenCalledWith({
                    ...uiInitialState,
                    activeTab: 2
                });
            });
        });

        describe('toggleSidebar', () => {
            it('should toggle sidebar visibility and persist to localStorage', () => {
                const initialState = store.isSidebarOpen();
                store.toggleSidebar();

                expect(store.isSidebarOpen()).toBe(!initialState);
                expect(saveStoreUIState).toHaveBeenCalledWith({
                    ...uiInitialState,
                    isSidebarOpen: !initialState
                });
            });

            it('should toggle back to original state when called twice', () => {
                const initialState = store.isSidebarOpen();
                store.toggleSidebar();
                store.toggleSidebar();

                expect(store.isSidebarOpen()).toBe(initialState);
            });
        });

        describe('setActiveSidebarTab', () => {
            it('should update active sidebar tab and persist to localStorage', () => {
                store.setActiveSidebarTab(1);

                expect(store.activeSidebarTab()).toBe(1);
                expect(saveStoreUIState).toHaveBeenCalledWith({
                    ...uiInitialState,
                    activeSidebarTab: 1
                });
            });
        });
    });
});
