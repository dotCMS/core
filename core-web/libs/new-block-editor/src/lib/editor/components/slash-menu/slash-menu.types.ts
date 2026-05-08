import type { ChainedCommands, Editor } from '@tiptap/core';

export interface BlockItem {
    label: string;
    description: string;
    icon?: string;
    keywords: string[];
    /**
     * When true, the slash trigger text is NOT deleted from the editor on selection.
     * The Tiptap suggestion session stays alive, so keyboard navigation keeps working.
     * The item's onSelect is responsible for cleaning up the range later.
     */
    keepRange?: boolean;
    /**
     * When true, choosing this row only clears the slash trigger and closes the menu
     * (no document insert / no drill-down). Used for empty and error rows in submenus.
     */
    isEmptyState?: boolean;
    /** Canonical block name used for allowedBlocks filtering. Absent = always shown. */
    blockName?: string;
    apply?: (chain: ChainedCommands) => ChainedCommands;
    onSelect?: (editor: Editor, range?: { from: number; to: number }) => void;
}
