import {
    patchState,
    signalStoreFeature,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed, Signal } from '@angular/core';

/**
 * Deep clone utility - uses structuredClone if available, falls back to JSON
 *
 * WHY DEEP CLONING IS NEEDED:
 * Without deep cloning, history entries would store references to the same objects.
 * When you modify the current state, those mutations would also affect past history entries,
 * corrupting the time machine's ability to restore previous states accurately.
 *
 * Example problem without deep cloning:
 * ```typescript
 * const state = { name: 'John', items: ['apple'] };
 * store.addHistory(state);  // Stores reference
 * state.items.push('banana'); // Mutates the object
 * store.undo(); // Returns corrupted state with ['apple', 'banana'] ❌
 * ```
 *
 * With deep cloning:
 * ```typescript
 * const state = { name: 'John', items: ['apple'] };
 * store.addHistory(state);  // Stores deep copy
 * state.items.push('banana'); // Only affects original
 * store.undo(); // Returns original ['apple'] ✅
 * ```
 *
 * You can disable deep cloning (deepClone: false) if:
 * - States are guaranteed to be immutable (e.g., using Immer, Immutable.js)
 * - Performance is critical and you can guarantee no mutations occur
 */
type StructuredCloneFunc = <T>(v: T) => T;
const safeClone: StructuredCloneFunc =
    typeof structuredClone !== 'undefined'
        ? structuredClone
        : <T>(v: T) => JSON.parse(JSON.stringify(v));

/**
 * Represents the state of the time machine history feature.
 *
 * @template T - The type of state snapshots being managed.
 * @property {T[]} history - An array of state snapshots representing the history stack.
 * @property {number} pointer - The current index within the history array (0-based).
 */
interface TimeMachineState<T> {
    history: T[];
    pointer: number;
}

/**
 * Interface defining methods for managing undo/redo history in a time machine pattern.
 *
 * @template T The type of the state being stored in the time machine.
 */
export interface TimeMachineMethods<T> {
    /**
     * Add a new state snapshot to the history.
     * If the pointer is not at the end, discards future states ("rebase").
     *
     * @param {T} state - The state snapshot to add to history.
     * @returns {void}
     */
    addHistory(state: T): void;

    /**
     * Navigate directly to a specific index in the history.
     *
     * @param {number} index - The index to navigate to (0-based).
     * @returns {(T | null)} The state at that index, or null if invalid.
     */
    goTo(index: number): T | null;

    /**
     * Move back one step in the history (undo).
     *
     * @returns {(T | null)} The previous state, or null if at the beginning or only one history item.
     */
    undo(): T | null;

    /**
     * Move forward one step in the history (redo).
     *
     * @returns {(T | null)} The next state, or null if at the end.
     */
    redo(): T | null;

    /**
     * Retrieve the state at a specific index without navigating.
     *
     * @param {number} index - The index to get the state from.
     * @returns {(T | null)} The state at that index, or null if invalid.
     */
    getStateAt(index: number): T | null;

    /**
     * Clear all history and reset the pointer.
     *
     * @returns {void}
     */
    clearHistory(): void;

    /**
     * Get the full history array (read-only).
     *
     * @returns {readonly T[]} The history array.
     */
    getHistory(): readonly T[];
}

/**
 * Represents the computed properties added by withTimeMachine.
 *
 * @template T - The type of state snapshots being managed.
 * @property {Signal<boolean>} haveHistory - Whether there is any history.
 * @property {Signal<boolean>} canUndo - Whether undo is possible.
 * @property {Signal<boolean>} canRedo - Whether redo is possible.
 * @property {Signal<T | undefined>} current - The current state at the pointer position.
 * @property {Signal<number>} currentIndex - The current pointer index.
 * @property {Signal<number>} historyLength - The total number of states in history.
 */
export interface TimeMachineComputed<T> {
    /**
     * Whether there is any history
     */
    haveHistory: Signal<boolean>;

    /**
     * Whether undo is possible (requires at least 2 history items)
     */
    canUndo: Signal<boolean>;

    /**
     * Whether redo is possible
     */
    canRedo: Signal<boolean>;

    /**
     * Current state at the pointer position
     */
    current: Signal<T | undefined>;

    /**
     * Current pointer index
     */
    currentIndex: Signal<number>;

    /**
     * Total number of states in history
     */
    historyLength: Signal<number>;

    /**
     * Whether pointer is at the beginning (index 0)
     */
    isAtStart: Signal<boolean>;

    /**
     * Whether pointer is at the end (last index)
     */
    isAtEnd: Signal<boolean>;
}

/**
 * Options for configuring the time machine
 */
export interface TimeMachineOptions {
    /**
     * Maximum number of history entries to keep (default: 100)
     * When exceeded, oldest entries are removed
     */
    maxHistory?: number;

    /**
     * Whether to deep clone states when adding to history (default: true)
     *
     * Deep cloning prevents mutation issues: if you store a reference to a state object
     * and later modify it, those changes would affect all history entries. Deep cloning
     * ensures each history entry is an independent snapshot.
     *
     * Set to false ONLY if:
     * - States are guaranteed immutable (e.g., using Immer, Immutable.js)
     * - Performance is critical and you can guarantee no mutations occur
     * - States contain only primitives (no nested objects/arrays)
     *
     * Default: true (recommended for safety)
     */
    deepClone?: boolean;
}

/**
 * Time machine feature for Signal Store
 * Adds history tracking and navigation capabilities to any store
 *
 * @example
 * ```typescript
 * export const myStore = signalStore(
 *   withState({ count: 0 }),
 *   withTimeMachine<{ count: number }>({ maxHistory: 50 }),
 *   withMethods(store => ({
 *     increment() {
 *       patchState(store, { count: store.count() + 1 });
 *       // Manually register state
 *       store.addHistory({ count: store.count() });
 *     }
 *   }))
 * );
 * ```
 */
export function withTimeMachine<T>(options?: TimeMachineOptions) {
    const maxHistory = options?.maxHistory ?? 100;
    const shouldDeepClone = options?.deepClone !== false; // default true

    return signalStoreFeature(
        withState<TimeMachineState<T>>({
            history: [],
            pointer: -1
        }),
        withComputed((store) => {
            const historyLength = computed(() => store.history().length);
            const currentIndex = computed(() => store.pointer());

            return {
                haveHistory: computed(() => historyLength() > 0),
                // Can undo only if there are at least 2 history items and pointer > 0
                canUndo: computed(() => {
                    const length = historyLength();
                    const index = currentIndex();
                    return length >= 2 && index > 0;
                }),
                // Can redo only if pointer is not at the last index
                canRedo: computed(() => {
                    const length = historyLength();
                    const index = currentIndex();
                    return index < length - 1;
                }),
                current: computed(() => {
                    const idx = currentIndex();
                    const hist = store.history();
                    return idx >= 0 && idx < hist.length ? hist[idx] : undefined;
                }),
                currentIndex,
                historyLength,
                isAtStart: computed(() => currentIndex() === 0),
                isAtEnd: computed(() => currentIndex() === historyLength() - 1)
            };
        }),
        withMethods((store) => {
            /**
             * Trim history to maxHistory length, keeping the most recent entries
             */
            const trimHistory = (history: T[]): T[] => {
                if (history.length <= maxHistory) return history;
                return history.slice(history.length - maxHistory);
            };

            /**
             * Validate and clamp index to valid range
             */
            const clampIndex = (index: number, length: number): number => {
                return Math.max(0, Math.min(index, length - 1));
            };

            return {
                addHistory: (state: T): void => {
                    const currentHistory = store.history();
                    const currentPointer = store.pointer();

                    // Deep clone the state to prevent mutation issues:
                    // Without cloning, modifying the current state would also mutate
                    // all previous history entries, corrupting the time machine.
                    // This ensures each history entry is an independent snapshot.
                    const stateToAdd = shouldDeepClone ? safeClone(state) : state;

                    // If pointer is not at the end, discard future states (rebase)
                    const baseHistory = currentHistory.slice(0, currentPointer + 1);
                    baseHistory.push(stateToAdd);

                    // Trim history if needed
                    const trimmedHistory = trimHistory(baseHistory);
                    const newPointer = trimmedHistory.length - 1;

                    patchState(store, {
                        history: trimmedHistory,
                        pointer: newPointer
                    });
                },

                goTo: (index: number): T | null => {
                    const history = store.history();
                    const validIndex = clampIndex(index, history.length);

                    if (validIndex < 0 || validIndex >= history.length) {
                        return null;
                    }

                    patchState(store, { pointer: validIndex });
                    return history[validIndex] ?? null;
                },

                undo: (): T | null => {
                    const history = store.history();
                    const currentPointer = store.pointer();

                    // Validation: Cannot undo if only one history item or at the beginning
                    if (history.length < 2 || currentPointer <= 0) {
                        return null;
                    }

                    const newPointer = currentPointer - 1;
                    patchState(store, { pointer: newPointer });

                    return history[newPointer] ?? null;
                },

                redo: (): T | null => {
                    const history = store.history();
                    const currentPointer = store.pointer();

                    // Validation: Cannot redo if at the last state
                    if (currentPointer >= history.length - 1) {
                        return null;
                    }

                    const newPointer = currentPointer + 1;
                    patchState(store, { pointer: newPointer });

                    return history[newPointer] ?? null;
                },

                getStateAt: (index: number): T | null => {
                    const history = store.history();
                    const validIndex = clampIndex(index, history.length);

                    if (validIndex < 0 || validIndex >= history.length) {
                        return null;
                    }

                    return history[validIndex] ?? null;
                },

                clearHistory: (): void => {
                    patchState(store, {
                        history: [],
                        pointer: -1
                    });
                },

                getHistory: (): readonly T[] => {
                    return store.history();
                }
            };
        })
    );
}
