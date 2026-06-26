import { signalStoreFeature, type, withComputed } from '@ngrx/signals';
import { on, withReducer } from '@ngrx/signals/events';

import { computed } from '@angular/core';

import { ImageEditorState } from '../../models/image-editor.models';
import { imageEditorHistoryEvents } from '../image-editor.events';
import {
    initialEditableSlices,
    rebuildHistory,
    restoreSlices,
    slicesAtIndex
} from '../image-editor.store-utils';

/**
 * History feature: the removable applied-edits list and undo / redo / reset.
 * Removing an entry replays the survivors so its effect is folded out; undo/redo
 * move the head and restore the corresponding snapshot. Exposes the user-facing
 * `appliedEdits` list plus the `canUndo` / `canRedo` flags.
 */
export function withHistory() {
    return signalStoreFeature(
        type<{ state: ImageEditorState }>(),
        withReducer(
            on(imageEditorHistoryEvents.editRemoved, ({ payload }, state) => {
                const removedIdx = state.history.findIndex((entry) => entry.id === payload.id);

                if (removedIdx === -1) {
                    return state;
                }

                // Replay the survivors so the removed edit's effect is folded out (a
                // plain filter left it baked into later snapshots).
                const history = rebuildHistory(state.history, removedIdx);
                // Preserve the logical head: shift it back only when an entry at or
                // before the head was removed, never jumping it forward to the tail.
                const historyIndex =
                    removedIdx <= state.historyIndex
                        ? Math.max(-1, state.historyIndex - 1)
                        : Math.min(state.historyIndex, history.length - 1);

                return {
                    ...state,
                    ...slicesAtIndex(history, historyIndex),
                    history,
                    historyIndex,
                    previewStatus: 'loading' as const,
                    cacheBust: state.cacheBust + 1
                };
            }),
            on(imageEditorHistoryEvents.undoRequested, (_event, state) => {
                const historyIndex = Math.max(-1, state.historyIndex - 1);

                return {
                    ...state,
                    ...slicesAtIndex(state.history, historyIndex),
                    historyIndex,
                    previewStatus: 'loading' as const,
                    cacheBust: state.cacheBust + 1
                };
            }),
            on(imageEditorHistoryEvents.redoRequested, (_event, state) => {
                const historyIndex = Math.min(state.history.length - 1, state.historyIndex + 1);

                return {
                    ...state,
                    ...slicesAtIndex(state.history, historyIndex),
                    historyIndex,
                    previewStatus: 'loading' as const,
                    cacheBust: state.cacheBust + 1
                };
            }),
            on(imageEditorHistoryEvents.resetRequested, (_event, state) => ({
                ...state,
                ...restoreSlices(initialEditableSlices),
                history: [],
                historyIndex: -1,
                previewStatus: 'loading' as const,
                cacheBust: state.cacheBust + 1
            }))
        ),
        withComputed((store) => ({
            /** The applied edits up to the current history head, for the edits list. */
            appliedEdits: computed(() =>
                store
                    .history()
                    .slice(0, store.historyIndex() + 1)
                    .map(({ id, category, label }) => ({ id, category, label }))
            ),
            /** Whether there is a previous history step to undo to. */
            canUndo: computed(() => store.historyIndex() > -1),
            /** Whether there is a forward history step to redo to. */
            canRedo: computed(() => store.historyIndex() < store.history().length - 1)
        }))
    );
}
