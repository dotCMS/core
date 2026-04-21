import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';

import { computed } from '@angular/core';


interface EditorState {
    /** Active language ID used for dotCMS API queries. */
    languageId: number;
    /**
     * Block types the editor is restricted to.
     * An empty array means all blocks are allowed.
     */
    allowedBlocks: string[];
}

const initialState: EditorState = {
    languageId: 1,
    allowedBlocks: []
};

export const EditorStore = signalStore(
    withState(initialState),

    withComputed(({ allowedBlocks }) => ({
        /** Set of allowed block names for O(1) lookups. Null when all blocks are allowed. */
        allowedBlocksSet: computed(() => {
            const blocks = allowedBlocks();
            return blocks.length > 0 ? new Set(blocks) : null;
        })
    })),

    withMethods((store) => ({
        setLanguageId(languageId: number): void {
            patchState(store, { languageId });
        },

        setAllowedBlocks(allowedBlocks: string[]): void {
            patchState(store, { allowedBlocks });
        },

        /**
         * Returns true when the given block type is allowed.
         * Always true when `allowedBlocks` is empty (all blocks permitted).
         */
        isAllowed(block: string): boolean {
            const set = store.allowedBlocksSet();
            return !set || set.has(block);
        }
    }))
);

export type EditorStore = InstanceType<typeof EditorStore>;
