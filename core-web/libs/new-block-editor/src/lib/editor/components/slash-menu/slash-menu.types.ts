import type { ChainedCommands, Editor } from '@tiptap/core';

export interface BlockItem {
    label: string;
    description: string;
    icon?: string;
    /**
     * How {@link icon} should be rendered.
     *
     * `'material'` (the default) treats it as a Material Symbols ligature NAME and applies that
     * font plus the bordered icon box every block row uses. `'glyph'` treats it as the character
     * itself — no box, no border, no ligature font, since applying `material-symbols-outlined`
     * to an emoji sets a font-family and variation settings meant for something else entirely.
     *
     * Explicit rather than inferred from the string: a reader of the template should see the
     * intent, not deduce it from a regex.
     */
    iconKind?: 'material' | 'glyph';
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
