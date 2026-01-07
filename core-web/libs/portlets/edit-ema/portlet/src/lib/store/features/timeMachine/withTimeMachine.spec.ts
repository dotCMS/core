import { describe, expect, it } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';

import { withTimeMachine } from './withTimeMachine';

interface TestState {
    count: number;
    items: string[];
}

const initialState: TestState = {
    count: 0,
    items: []
};

export const testStoreMock = signalStore(
    { protectedState: false },
    withState<TestState>(initialState),
    withTimeMachine<TestState>()
);

const storeNoClone = signalStore(
    { protectedState: false },
    withState<TestState>(initialState),
    withTimeMachine<TestState>({ deepClone: false })
);

describe('withTimeMachine', () => {
    let spectator: SpectatorService<InstanceType<typeof testStoreMock>>;
    let store: InstanceType<typeof testStoreMock>;

    const createService = createServiceFactory({
        service: testStoreMock
    });

    const createServiceNoClone = createServiceFactory({
        service: storeNoClone
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    describe('Initial state', () => {
        it('should have empty history and pointer at -1', () => {
            expect(store.historyLength()).toBe(0);
            expect(store.currentIndex()).toBe(-1);
            expect(store.haveHistory()).toBe(false);
            expect(store.canUndo()).toBe(false);
            expect(store.canRedo()).toBe(false);
            expect(store.current()).toBeUndefined();
        });
    });

    describe('addHistory', () => {
        it('should add state to history', () => {
            const state1: TestState = { count: 1, items: ['apple'] };
            store.addHistory(state1);

            expect(store.historyLength()).toBe(1);
            expect(store.currentIndex()).toBe(0);
            expect(store.haveHistory()).toBe(true);
            expect(store.current()).toEqual(state1);
        });

        it('should update pointer when adding new history', () => {
            const state1: TestState = { count: 1, items: ['apple'] };
            const state2: TestState = { count: 2, items: ['banana'] };

            store.addHistory(state1);
            expect(store.currentIndex()).toBe(0);

            store.addHistory(state2);
            expect(store.currentIndex()).toBe(1);
            expect(store.current()).toEqual(state2);
        });

        it('should discard future states when adding history in the middle', () => {
            const state1: TestState = { count: 1, items: ['apple'] };
            const state2: TestState = { count: 2, items: ['banana'] };
            const state3: TestState = { count: 3, items: ['cherry'] };

            store.addHistory(state1);
            store.addHistory(state2);
            store.undo(); // Go back to state1 (index 0)

            store.addHistory(state3); // Should discard state2

            expect(store.historyLength()).toBe(2);
            expect(store.currentIndex()).toBe(1);
            expect(store.current()).toEqual(state3);
            expect(store.getStateAt(0)).toEqual(state1);
            expect(store.getStateAt(1)).toEqual(state3);
        });

        it('should deep clone state to prevent mutations', () => {
            const state: TestState = { count: 1, items: ['apple'] };
            store.addHistory(state);

            // Mutate the original state
            state.items.push('banana');
            state.count = 999;

            // History should not be affected
            expect(store.current()?.items).toEqual(['apple']);
            expect(store.current()?.count).toBe(1);
        });
    });

    describe('undo', () => {
        it('should return null if no history', () => {
            expect(store.undo()).toBeNull();
        });

        it('should return null if only one history item', () => {
            store.addHistory({ count: 1, items: ['apple'] });
            expect(store.undo()).toBeNull();
        });

        it('should move pointer back and return previous state', () => {
            const state1: TestState = { count: 1, items: ['apple'] };
            const state2: TestState = { count: 2, items: ['banana'] };

            store.addHistory(state1);
            store.addHistory(state2);

            expect(store.currentIndex()).toBe(1);
            const previousState = store.undo();

            expect(previousState).toEqual(state1);
            expect(store.currentIndex()).toBe(0);
            expect(store.current()).toEqual(state1);
        });

        it('should return null when at the beginning', () => {
            const state1: TestState = { count: 1, items: ['apple'] };
            const state2: TestState = { count: 2, items: ['banana'] };

            store.addHistory(state1);
            store.addHistory(state2);
            store.undo(); // Now at index 0

            expect(store.undo()).toBeNull();
            expect(store.currentIndex()).toBe(0);
        });
    });

    describe('redo', () => {
        it('should return null if at the end', () => {
            const state1: TestState = { count: 1, items: ['apple'] };
            store.addHistory(state1);
            expect(store.redo()).toBeNull();
        });

        it('should move pointer forward and return next state', () => {
            const state1: TestState = { count: 1, items: ['apple'] };
            const state2: TestState = { count: 2, items: ['banana'] };

            store.addHistory(state1);
            store.addHistory(state2);
            store.undo(); // Go back to state1

            const nextState = store.redo();

            expect(nextState).toEqual(state2);
            expect(store.currentIndex()).toBe(1);
            expect(store.current()).toEqual(state2);
        });
    });

    describe('goTo', () => {
        it('should navigate to specific index', () => {
            store.addHistory({ count: 1, items: ['apple'] });
            store.addHistory({ count: 2, items: ['banana'] });
            store.addHistory({ count: 3, items: ['cherry'] });

            const state = store.goTo(0);
            expect(store.currentIndex()).toBe(0);
            expect(state?.count).toBe(1);

            const state2 = store.goTo(2);
            expect(store.currentIndex()).toBe(2);
            expect(state2?.count).toBe(3);
        });

        it('should clamp index to valid range', () => {
            store.addHistory({ count: 1, items: ['apple'] });
            store.addHistory({ count: 2, items: ['banana'] });

            store.goTo(-1);
            expect(store.currentIndex()).toBe(0);

            store.goTo(100);
            expect(store.currentIndex()).toBe(1);
        });

        it('should return null for invalid index', () => {
            expect(store.goTo(0)).toBeNull();
        });
    });

    describe('getStateAt', () => {
        it('should return state at specific index without navigating', () => {
            const state1: TestState = { count: 1, items: ['apple'] };
            const state2: TestState = { count: 2, items: ['banana'] };

            store.addHistory(state1);
            store.addHistory(state2);

            expect(store.currentIndex()).toBe(1);
            expect(store.getStateAt(0)).toEqual(state1);
            expect(store.getStateAt(1)).toEqual(state2);
            expect(store.currentIndex()).toBe(1); // Should not change
        });

        it('should return null for invalid index', () => {
            expect(store.getStateAt(0)).toBeNull();
            expect(store.getStateAt(-1)).toBeNull();
        });
    });

    describe('clearHistory', () => {
        it('should clear all history and reset pointer', () => {
            store.addHistory({ count: 1, items: ['apple'] });
            store.addHistory({ count: 2, items: ['banana'] });

            store.clearHistory();

            expect(store.historyLength()).toBe(0);
            expect(store.currentIndex()).toBe(-1);
            expect(store.haveHistory()).toBe(false);
        });
    });

    describe('Computed properties', () => {
        it('should correctly compute canUndo', () => {
            expect(store.canUndo()).toBe(false);

            store.addHistory({ count: 1, items: ['apple'] });
            expect(store.canUndo()).toBe(false); // Need at least 2 items

            store.addHistory({ count: 2, items: ['banana'] });
            expect(store.canUndo()).toBe(true);

            store.undo();
            expect(store.canUndo()).toBe(false); // At start
        });

        it('should correctly compute canRedo', () => {
            expect(store.canRedo()).toBe(false);

            store.addHistory({ count: 1, items: ['apple'] });
            store.addHistory({ count: 2, items: ['banana'] });
            expect(store.canRedo()).toBe(false); // At end

            store.undo();
            expect(store.canRedo()).toBe(true);
        });

        it('should correctly compute isAtStart and isAtEnd', () => {
            store.addHistory({ count: 1, items: ['apple'] });
            store.addHistory({ count: 2, items: ['banana'] });

            expect(store.isAtStart()).toBe(false);
            expect(store.isAtEnd()).toBe(true);

            store.undo();
            expect(store.isAtStart()).toBe(true);
            expect(store.isAtEnd()).toBe(false);
        });
    });

    describe('Deep cloning', () => {
        it('should deep clone by default', () => {
            const state: TestState = { count: 1, items: ['apple'] };
            store.addHistory(state);

            state.items.push('banana');
            state.count = 999;

            expect(store.current()?.items).toEqual(['apple']);
            expect(store.current()?.count).toBe(1);
        });

        it('should allow disabling deep cloning', () => {
            const spectatorNoClone = createServiceNoClone();
            const storeNoCloneInstance = spectatorNoClone.service;

            const state: TestState = { count: 1, items: ['apple'] };
            storeNoCloneInstance.addHistory(state);

            state.items.push('banana');

            // Without deep cloning, mutations affect history
            expect(storeNoCloneInstance.current()?.items).toEqual(['apple', 'banana']);
        });
    });
});
