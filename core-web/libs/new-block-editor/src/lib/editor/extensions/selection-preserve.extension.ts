import { Extension } from '@tiptap/core';
import { Plugin, PluginKey } from '@tiptap/pm/state';
import { Decoration, DecorationSet } from '@tiptap/pm/view';

interface PreservedRange {
    from: number;
    to: number;
}

/**
 * Highlights the cursor's block while a focus-stealing dialog is open. Skips non-empty
 * blocks (their text already has the native selection highlight when re-focused).
 */
export const SELECTION_PRESERVE_KEY = new PluginKey<PreservedRange | null>('selectionPreserve');

/**
 * Highlights the FULL block at the cursor regardless of content. Driven by toolbar pop-ups
 * (e.g. block-type select) where the user needs to see "this is the node about to change".
 */
export const BLOCK_TARGET_KEY = new PluginKey<PreservedRange | null>('blockTarget');

/**
 * Highlights the EXACT selected text range while the link popover is open in insert mode.
 * Once the popover's URL input steals focus the browser stops painting the editor's native
 * `::selection`, so the author loses sight of which words will become the link. This inline
 * decoration persists through the blur and marks precisely the selection (not the whole
 * block, which `BLOCK_TARGET_KEY` does). Edit mode is handled separately via the
 * `.link-editing` class on the anchor element.
 */
export const LINK_SELECTION_KEY = new PluginKey<PreservedRange | null>('linkSelection');

function decorateRange(
    state: Parameters<NonNullable<Plugin['props']['decorations']>>[0],
    range: PreservedRange | null,
    className: string
): DecorationSet {
    if (!range) return DecorationSet.empty;
    const decos: Decoration[] = [];
    state.doc.nodesBetween(range.from, range.to, (node, pos) => {
        if (state.doc.resolve(pos).depth !== 0) return true;
        if (!node.isBlock) return false;
        decos.push(Decoration.node(pos, pos + node.nodeSize, { class: className }));
        return false;
    });
    return DecorationSet.create(state.doc, decos);
}

export const SelectionPreserveExtension = Extension.create({
    name: 'selectionPreserve',

    addProseMirrorPlugins() {
        return [
            // Plugin 1: empty-only highlight (dialogs)
            new Plugin({
                key: SELECTION_PRESERVE_KEY,
                state: {
                    init: () => null,
                    apply(tr, prev) {
                        const meta = tr.getMeta(SELECTION_PRESERVE_KEY) as
                            | { active: boolean }
                            | undefined;
                        if (!meta) return prev;
                        if (!meta.active) return null;
                        const { from } = tr.selection;
                        const resolved = tr.doc.resolve(from);
                        const block = resolved.depth >= 1 ? resolved.node(1) : null;
                        if (!block || block.content.size > 0) return null;
                        return { from, to: from };
                    }
                },
                props: {
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    decorations(state): any {
                        return decorateRange(
                            state,
                            SELECTION_PRESERVE_KEY.getState(state) ?? null,
                            'editor-selection-preserved'
                        );
                    }
                }
            }),

            // Plugin 2: full-block highlight (toolbar pop-ups)
            new Plugin({
                key: BLOCK_TARGET_KEY,
                state: {
                    init: () => null,
                    apply(tr, prev) {
                        const meta = tr.getMeta(BLOCK_TARGET_KEY) as
                            | { active: boolean }
                            | undefined;
                        if (!meta) return prev;
                        if (!meta.active) return null;
                        // Capture the whole top-level block at the cursor — content or not.
                        const { from, to } = tr.selection;
                        return { from, to };
                    }
                },
                props: {
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    decorations(state): any {
                        return decorateRange(
                            state,
                            BLOCK_TARGET_KEY.getState(state) ?? null,
                            'editor-block-target'
                        );
                    }
                }
            }),

            // Plugin 3: exact text-range highlight (link popover, insert mode)
            new Plugin({
                key: LINK_SELECTION_KEY,
                state: {
                    init: () => null,
                    apply(tr, prev) {
                        const meta = tr.getMeta(LINK_SELECTION_KEY) as
                            | { active: boolean }
                            | undefined;
                        if (!meta) return prev;
                        if (!meta.active) return null;
                        // Snapshot the current text selection; a collapsed caret has nothing
                        // to highlight, so skip it.
                        const { from, to } = tr.selection;
                        return from === to ? null : { from, to };
                    }
                },
                props: {
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    decorations(state): any {
                        const range = LINK_SELECTION_KEY.getState(state) ?? null;
                        if (!range) return DecorationSet.empty;
                        return DecorationSet.create(state.doc, [
                            Decoration.inline(range.from, range.to, {
                                class: 'editor-link-selection'
                            })
                        ]);
                    }
                }
            })
        ];
    }
});
