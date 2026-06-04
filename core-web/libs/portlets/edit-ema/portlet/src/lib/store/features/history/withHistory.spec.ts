import { signalStore, withState } from '@ngrx/signals';

import { withHistory } from './withHistory';

// Mock structuredClone for test environment
// eslint-disable-next-line @typescript-eslint/no-explicit-any
global.structuredClone = (obj: any) => JSON.parse(JSON.stringify(obj));

interface TestState {
    value: string;
    nested?: { data: string };
}

describe('withHistory', () => {
    describe('initial state', () => {
        it('should start with empty history', () => {
            const Store = signalStore(
                withState<{ testState: TestState | null }>({ testState: null }),
                withHistory<TestState>({
                    selector: (store) => store.testState()
                })
            );

            const store = new Store();

            expect(store.history()).toEqual([]);
            expect(store.historyPointer()).toBe(-1);
            expect(store.historyLength()).toBe(0);
        });

        it('should initialize computed signals correctly', () => {
            const Store = signalStore(
                withState<{ testState: TestState | null }>({ testState: null }),
                withHistory<TestState>({
                    selector: (store) => store.testState()
                })
            );

            const store = new Store();

            expect(store.canUndo()).toBe(false);
            expect(store.canRedo()).toBe(false);
            expect(store.isAtStart()).toBe(false); // -1 is not at start
            expect(store.isAtEnd()).toBe(false);
        });
    });

    describe('addToHistory', () => {
        it('should add state to history', () => {
            const Store = signalStore(
                withState<{ testState: TestState | null }>({ testState: null }),
                withHistory<TestState>({
                    selector: (store) => store.testState()
                })
            );

            const store = new Store();
            const state1: TestState = { value: 'test1' };

            store.addToHistory(state1);

            expect(store.history().length).toBe(1);
            expect(store.history()[0]).toEqual(state1);
            expect(store.historyPointer()).toBe(0);
        });

        it('should add multiple states to history', () => {
            const Store = signalStore(
                withState<{ testState: TestState | null }>({ testState: null }),
                withHistory<TestState>({
                    selector: (store) => store.testState()
                })
            );

            const store = new Store();
            const state1: TestState = { value: 'test1' };
            const state2: TestState = { value: 'test2' };
            const state3: TestState = { value: 'test3' };

            store.addToHistory(state1);
            store.addToHistory(state2);
            store.addToHistory(state3);

            expect(store.history().length).toBe(3);
            expect(store.historyPointer()).toBe(2);
            expect(store.history()[0]).toEqual(state1);
            expect(store.history()[1]).toEqual(state2);
            expect(store.history()[2]).toEqual(state3);
        });

        it('should trim future history when adding at non-end position (branching)', () => {
            const Store = signalStore(
                withState<{ testState: TestState | null }>({ testState: null }),
                withHistory<TestState>({
                    selector: (store) => store.testState()
                })
            );

            const store = new Store();
            const state1: TestState = { value: 'test1' };
            const state2: TestState = { value: 'test2' };
            const state3: TestState = { value: 'test3' };
            const newState: TestState = { value: 'new' };

            // Add 3 states
            store.addToHistory(state1);
            store.addToHistory(state2);
            store.addToHistory(state3);

            // Go back to state1
            store.undo();
            store.undo();

            // Add new state (should trim state2 and state3)
            store.addToHistory(newState);

            expect(store.history().length).toBe(2);
            expect(store.history()[0]).toEqual(state1);
            expect(store.history()[1]).toEqual(newState);
            expect(store.historyPointer()).toBe(1);
        });

        it('should respect maxHistory limit', () => {
            const Store = signalStore(
                withState<{ testState: TestState | null }>({ testState: null }),
                withHistory<TestState>({
                    selector: (store) => store.testState(),
                    maxHistory: 3
                })
            );

            const store = new Store();

            // Add 5 states (max is 3)
            for (let i = 0; i < 5; i++) {
                store.addToHistory({ value: `test${i}` });
            }

            expect(store.history().length).toBe(3);
            // Should keep most recent 3
            expect(store.history()[0].value).toBe('test2');
            expect(store.history()[1].value).toBe('test3');
            expect(store.history()[2].value).toBe('test4');
        });

        it('should deep clone by default', () => {
            const Store = signalStore(
                withState<{ testState: TestState | null }>({ testState: null }),
                withHistory<TestState>({
                    selector: (store) => store.testState()
                })
            );

            const store = new Store();
            const state: TestState = { value: 'test', nested: { data: 'original' } };

            store.addToHistory(state);

            // Mutate original
            if (state.nested) {
                state.nested.data = 'mutated';
            }

            // History should be unchanged (deep clone)
            const historyEntry = store.history()[0];
            expect(historyEntry.nested?.data).toBe('original');
        });

        it('should not clone when deepClone is false', () => {
            const Store = signalStore(
                withState<{ testState: TestState | null }>({ testState: null }),
                withHistory<TestState>({
                    selector: (store) => store.testState(),
                    deepClone: false
                })
            );

            const store = new Store();
            const state: TestState = { value: 'test', nested: { data: 'original' } };

            store.addToHistory(state);

            // Mutate original
            if (state.nested) {
                state.nested.data = 'mutated';
            }

            // History should reflect mutation (no clone)
            const historyEntry = store.history()[0];
            expect(historyEntry.nested?.data).toBe('mutated');
        });
    });

    describe('undo', () => {
        it('should navigate to previous state', () => {
            const Store = signalStore(
                withState<{ testState: TestState | null }>({ testState: null }),
                withHistory<TestState>({
                    selector: (store) => store.testState()
                })
            );

            const store = new Store();
            const state1: TestState = { value: 'test1' };
            const state2: TestState = { value: 'test2' };

            store.addToHistory(state1);
            store.addToHistory(state2);

            const previous = store.undo();

            expect(previous).toEqual(state1);
            expect(store.historyPointer()).toBe(0);
        });

        it('should return null when at start', () => {
            const Store = signalStore(
                withState<{ testState: TestState | null }>({ testState: null }),
                withHistory<TestState>({
                    selector: (store) => store.testState()
                })
            );

            const store = new Store();
            const state1: TestState = { value: 'test1' };

            store.addToHistory(state1);

            const previous = store.undo();

            expect(previous).toBeNull();
            expect(store.historyPointer()).toBe(0);
        });

        it('should update canUndo computed signal', () => {
            const Store = signalStore(
                withState<{ testState: TestState | null }>({ testState: null }),
                withHistory<TestState>({
                    selector: (store) => store.testState()
                })
            );

            const store = new Store();
            const state1: TestState = { value: 'test1' };
            const state2: TestState = { value: 'test2' };

            store.addToHistory(state1);
            store.addToHistory(state2);

            expect(store.canUndo()).toBe(true);

            store.undo();

            expect(store.canUndo()).toBe(false); // Now at position 0
        });
    });

    describe('redo', () => {
        it('should navigate to next state', () => {
            const Store = signalStore(
                withState<{ testState: TestState | null }>({ testState: null }),
                withHistory<TestState>({
                    selector: (store) => store.testState()
                })
            );

            const store = new Store();
            const state1: TestState = { value: 'test1' };
            const state2: TestState = { value: 'test2' };

            store.addToHistory(state1);
            store.addToHistory(state2);

            store.undo();
            const next = store.redo();

            expect(next).toEqual(state2);
            expect(store.historyPointer()).toBe(1);
        });

        it('should return null when at end', () => {
            const Store = signalStore(
                withState<{ testState: TestState | null }>({ testState: null }),
                withHistory<TestState>({
                    selector: (store) => store.testState()
                })
            );

            const store = new Store();
            const state1: TestState = { value: 'test1' };

            store.addToHistory(state1);

            const next = store.redo();

            expect(next).toBeNull();
            expect(store.historyPointer()).toBe(0);
        });

        it('should update canRedo computed signal', () => {
            const Store = signalStore(
                withState<{ testState: TestState | null }>({ testState: null }),
                withHistory<TestState>({
                    selector: (store) => store.testState()
                })
            );

            const store = new Store();
            const state1: TestState = { value: 'test1' };
            const state2: TestState = { value: 'test2' };

            store.addToHistory(state1);
            store.addToHistory(state2);

            expect(store.canRedo()).toBe(false);

            store.undo();

            expect(store.canRedo()).toBe(true);

            store.redo();

            expect(store.canRedo()).toBe(false);
        });
    });

    describe('goTo', () => {
        it('should navigate to specific history index', () => {
            const Store = signalStore(
                withState<{ testState: TestState | null }>({ testState: null }),
                withHistory<TestState>({
                    selector: (store) => store.testState()
                })
            );

            const store = new Store();
            const state1: TestState = { value: 'test1' };
            const state2: TestState = { value: 'test2' };
            const state3: TestState = { value: 'test3' };

            store.addToHistory(state1);
            store.addToHistory(state2);
            store.addToHistory(state3);

            const result = store.goTo(1);

            expect(result).toEqual(state2);
            expect(store.historyPointer()).toBe(1);
        });

        it('should return null for invalid index', () => {
            const Store = signalStore(
                withState<{ testState: TestState | null }>({ testState: null }),
                withHistory<TestState>({
                    selector: (store) => store.testState()
                })
            );

            const store = new Store();
            const state1: TestState = { value: 'test1' };

            store.addToHistory(state1);

            expect(store.goTo(-1)).toBeNull();
            expect(store.goTo(5)).toBeNull();
            expect(store.historyPointer()).toBe(0); // Unchanged
        });
    });

    describe('clearHistory', () => {
        it('should clear all history', () => {
            const Store = signalStore(
                withState<{ testState: TestState | null }>({ testState: null }),
                withHistory<TestState>({
                    selector: (store) => store.testState()
                })
            );

            const store = new Store();
            const state1: TestState = { value: 'test1' };
            const state2: TestState = { value: 'test2' };

            store.addToHistory(state1);
            store.addToHistory(state2);

            store.clearHistory();

            expect(store.history()).toEqual([]);
            expect(store.historyPointer()).toBe(-1);
            expect(store.historyLength()).toBe(0);
            expect(store.canUndo()).toBe(false);
            expect(store.canRedo()).toBe(false);
        });
    });

    describe('getHistory', () => {
        it('should return readonly history array', () => {
            const Store = signalStore(
                withState<{ testState: TestState | null }>({ testState: null }),
                withHistory<TestState>({
                    selector: (store) => store.testState()
                })
            );

            const store = new Store();
            const state1: TestState = { value: 'test1' };
            const state2: TestState = { value: 'test2' };

            store.addToHistory(state1);
            store.addToHistory(state2);

            const history = store.getHistory();

            expect(history.length).toBe(2);
            expect(history[0]).toEqual(state1);
            expect(history[1]).toEqual(state2);
        });
    });

    describe('computed signals', () => {
        it('should correctly compute isAtStart and isAtEnd', () => {
            const Store = signalStore(
                withState<{ testState: TestState | null }>({ testState: null }),
                withHistory<TestState>({
                    selector: (store) => store.testState()
                })
            );

            const store = new Store();
            const state1: TestState = { value: 'test1' };
            const state2: TestState = { value: 'test2' };
            const state3: TestState = { value: 'test3' };

            store.addToHistory(state1);
            store.addToHistory(state2);
            store.addToHistory(state3);

            // At end
            expect(store.isAtEnd()).toBe(true);
            expect(store.isAtStart()).toBe(false);

            // Go to start
            store.undo();
            store.undo();

            expect(store.isAtStart()).toBe(true);
            expect(store.isAtEnd()).toBe(false);

            // Go to middle
            store.redo();

            expect(store.isAtStart()).toBe(false);
            expect(store.isAtEnd()).toBe(false);
        });
    });
});
