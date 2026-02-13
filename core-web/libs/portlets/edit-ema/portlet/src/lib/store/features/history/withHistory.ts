import { patchState, signalStoreFeature, withComputed, withMethods, withState } from '@ngrx/signals';

import { computed } from '@angular/core';

/**
 * Configuration for the history feature
 * @template T - The type of state being tracked
 */
export interface WithHistoryConfig<T> {
    /**
     * Selector function to get the current state value
     *
     * Note: Uses `any` for the store parameter to keep the API simple and avoid requiring callers
     * to cast. This is acceptable because:
     * 1. withHistory is a generic composition utility that works with any store type
     * 2. We only care about the return type (T | null), not the store's structure
     * 3. The alternative (adding a Store generic parameter) would complicate the API
     * 4. The selector is only used internally by the feature, not exposed to external callers
     */
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    selector: (store: any) => T | null;

    /**
     * Maximum number of history entries to keep
     * @default 50
     */
    maxHistory?: number;

    /**
     * Whether to deep clone state snapshots
     * Set to true for complex objects to prevent mutations
     * @default true
     */
    deepClone?: boolean;
}

/**
 * Generic history tracking feature for undo/redo functionality
 *
 * This is a reusable composition utility that can be added to any feature
 * requiring undo/redo capabilities. It tracks state snapshots and provides
 * navigation through history.
 *
 * @template T - The type of state being tracked
 *
 * @example Basic usage with page state
 * ```typescript
 * export function withPage() {
 *   return signalStoreFeature(
 *     withState<{ pageAssetResponse: PageAssetResponse | null }>(...),
 *
 *     // Compose with history - automatically tracks pageAssetResponse
 *     withHistory<PageAssetResponse>({
 *       selector: (store) => store.pageAssetResponse(),
 *       maxHistory: 50,
 *       deepClone: true
 *     }),
 *
 *     withMethods((store) => ({
 *       setPageAssetResponseOptimistic(response) {
 *         const current = store.pageAssetResponse();
 *         if (current) {
 *           store.addToHistory(current); // Track before changing
 *         }
 *         patchState(store, { pageAssetResponse: response });
 *       },
 *
 *       undoPageChange() {
 *         const previous = store.undo();
 *         if (previous) {
 *           patchState(store, { pageAssetResponse: previous });
 *           return true;
 *         }
 *         return false;
 *       }
 *     }))
 *   );
 * }
 * ```
 *
 * @example Future: Track editor state
 * ```typescript
 * export function withEditor() {
 *   return signalStoreFeature(
 *     withState<{ editorBounds: Container[] }>(...),
 *
 *     withHistory<Container[]>({
 *       selector: (store) => store.editorBounds(),
 *       maxHistory: 20
 *     }),
 *
 *     // Now drag/drop operations can be undone!
 *   );
 * }
 * ```
 */
export function withHistory<T>(config: WithHistoryConfig<T>) {
    return signalStoreFeature(
        withState<{
            history: T[];
            historyPointer: number;
        }>({
            history: [],
            historyPointer: -1
        }),

        withComputed((store) => ({
            /**
             * Can navigate backward in history
             */
            canUndo: computed(() => store.historyPointer() > 0),

            /**
             * Can navigate forward in history
             */
            canRedo: computed(() =>
                store.historyPointer() < store.history().length - 1
            ),

            /**
             * Total number of history entries
             */
            historyLength: computed(() => store.history().length),

            /**
             * At the beginning of history
             */
            isAtStart: computed(() => {
                const history = store.history();
                return history.length > 0 && store.historyPointer() === 0;
            }),

            /**
             * At the end of history
             */
            isAtEnd: computed(() => {
                const history = store.history();
                return history.length > 0 && store.historyPointer() === history.length - 1;
            })
        })),

        withMethods((store) => ({
            /**
             * Add current state to history
             * Call this BEFORE making changes to track the state
             */
            addToHistory(state: T) {
                const history = store.history();
                const pointer = store.historyPointer();
                const maxHistory = config.maxHistory ?? 50;

                // Clone to prevent mutations
                const snapshot = config.deepClone !== false
                    ? structuredClone(state)
                    : state;

                // Trim future history if we're not at the end (branching)
                const newHistory = [
                    ...history.slice(0, pointer + 1),
                    snapshot
                ];

                // Trim to max history (keep most recent)
                const trimmed = newHistory.slice(-maxHistory);

                patchState(store, {
                    history: trimmed,
                    historyPointer: trimmed.length - 1
                });
            },

            /**
             * Navigate to previous state in history
             * @returns The previous state, or null if at start
             */
            undo(): T | null {
                const pointer = store.historyPointer();
                if (pointer > 0) {
                    const newPointer = pointer - 1;
                    const previousState = store.history()[newPointer];

                    patchState(store, {
                        historyPointer: newPointer
                    });

                    return config.deepClone !== false
                        ? structuredClone(previousState)
                        : previousState;
                }
                return null;
            },

            /**
             * Navigate to next state in history
             * @returns The next state, or null if at end
             */
            redo(): T | null {
                const pointer = store.historyPointer();
                const history = store.history();

                if (pointer < history.length - 1) {
                    const newPointer = pointer + 1;
                    const nextState = history[newPointer];

                    patchState(store, {
                        historyPointer: newPointer
                    });

                    return config.deepClone !== false
                        ? structuredClone(nextState)
                        : nextState;
                }
                return null;
            },

            /**
             * Navigate to specific point in history
             * @param index - Zero-based index in history array
             * @returns The state at that index, or null if invalid
             */
            goTo(index: number): T | null {
                const history = store.history();
                if (index >= 0 && index < history.length) {
                    patchState(store, {
                        historyPointer: index
                    });

                    return config.deepClone !== false
                        ? structuredClone(history[index])
                        : history[index];
                }
                return null;
            },

            /**
             * Clear all history
             * Use when starting fresh (e.g., after save, on new page load)
             */
            clearHistory() {
                patchState(store, {
                    history: [],
                    historyPointer: -1
                });
            },

            /**
             * Get entire history array (for debugging/visualization)
             */
            getHistory(): readonly T[] {
                return store.history();
            }
        }))
    );
}
