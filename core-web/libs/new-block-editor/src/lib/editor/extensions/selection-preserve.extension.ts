import { Extension } from '@tiptap/core';
import { Plugin, PluginKey } from '@tiptap/pm/state';
import { Decoration, DecorationSet } from '@tiptap/pm/view';

interface SelectionPreserveState {
    from: number;
    to: number;
}

export const SELECTION_PRESERVE_KEY = new PluginKey<SelectionPreserveState | null>(
    'selectionPreserve'
);

export const SelectionPreserveExtension = Extension.create({
    name: 'selectionPreserve',

    addProseMirrorPlugins() {
        return [
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
                        // Only highlight empty blocks — non-empty content has its own visual selection
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
                        const pluginState = SELECTION_PRESERVE_KEY.getState(state);
                        if (!pluginState) return DecorationSet.empty;

                        const { from, to } = pluginState;
                        const decos: Decoration[] = [];

                        state.doc.nodesBetween(from, to, (node, pos) => {
                            if (state.doc.resolve(pos).depth !== 0) return true;
                            if (!node.isBlock) return false;
                            decos.push(
                                Decoration.node(pos, pos + node.nodeSize, {
                                    class: 'editor-selection-preserved'
                                })
                            );
                            return false;
                        });

                        return DecorationSet.create(state.doc, decos);
                    }
                }
            })
        ];
    }
});
