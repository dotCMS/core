import { Node, mergeAttributes } from '@tiptap/core';
import { TextSelection } from '@tiptap/pm/state';

import { GridResizePlugin } from '../grid-resize.plugin';

// Augmentation key is namespaced (not `gridBlock`) so it doesn't collide with
// the legacy block-editor's `gridBlock` declaration when both libs are present
// in the same compilation. Runtime command names (`insertGrid`, `setGridColumns`)
// are unaffected — they're set by `addCommands()` below.
declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        newBlockEditorGridBlock: {
            insertGrid: () => ReturnType;
            setGridColumns: (columns: number[]) => ReturnType;
        };
    }
}

// ── GridColumn ────────────────────────────────────────────────────────────────
// Width is controlled exclusively by the parent gridBlock's grid-template-columns.
// No span/width attribute lives here — gridBlock.columns is the single source of truth.

export const GridColumn = Node.create({
    name: 'gridColumn',
    group: 'gridColumnGroup',
    content: 'block+',
    isolating: true,

    parseHTML() {
        return [{ tag: 'div[data-type="gridColumn"]' }];
    },

    renderHTML({ HTMLAttributes }) {
        return ['div', mergeAttributes({ 'data-type': 'gridColumn' }, HTMLAttributes), 0];
    },

    addNodeView() {
        return () => {
            const dom = document.createElement('div');
            dom.setAttribute('data-type', 'gridColumn');
            dom.classList.add('grid-block__column');

            const contentDOM = document.createElement('div');
            contentDOM.classList.add('grid-block__column-content');
            dom.appendChild(contentDOM);

            return { dom, contentDOM };
        };
    }
});

// ── GridBlock ─────────────────────────────────────────────────────────────────

export const GridBlock = Node.create({
    name: 'gridBlock',
    group: 'block',
    content: 'gridColumn{2}',
    defining: true,
    draggable: true,

    addAttributes() {
        return {
            columns: {
                default: [6, 6],
                parseHTML: (element) => {
                    try {
                        const raw = element.getAttribute('data-columns');
                        const parsed = raw ? JSON.parse(raw) : null;

                        if (Array.isArray(parsed) && parsed.length === 2) return parsed;
                    } catch {
                        // ignore malformed JSON
                    }

                    return [6, 6];
                },
                renderHTML: (attributes) => {
                    const cols: number[] = Array.isArray(attributes['columns'])
                        ? (attributes['columns'] as number[])
                        : [6, 6];

                    return {
                        'data-columns': JSON.stringify(cols),
                        style: `grid-template-columns: ${cols[0]}fr ${cols[1]}fr`
                    };
                }
            }
        };
    },

    parseHTML() {
        return [{ tag: 'div[data-type="gridBlock"]' }];
    },

    renderHTML({ HTMLAttributes }) {
        return ['div', mergeAttributes({ 'data-type': 'gridBlock' }, HTMLAttributes), 0];
    },

    addNodeView() {
        return ({ node }) => {
            const dom = document.createElement('div');
            dom.setAttribute('data-type', 'gridBlock');
            dom.classList.add('grid-block');

            const cols: number[] = (node.attrs['columns'] as number[]) ?? [6, 6];
            dom.style.gridTemplateColumns = `${cols[0]}fr ${cols[1]}fr`;

            // contentDOM: display:contents so gridColumn cells participate directly
            // in the parent CSS Grid defined on dom.
            const contentDOM = document.createElement('div');
            contentDOM.classList.add('grid-block__grid');
            dom.appendChild(contentDOM);

            return {
                dom,
                contentDOM,
                update(updatedNode) {
                    if (updatedNode.type.name !== 'gridBlock') return false;

                    const c: number[] = (updatedNode.attrs['columns'] as number[]) ?? [6, 6];
                    dom.style.gridTemplateColumns = `${c[0]}fr ${c[1]}fr`;

                    return true;
                }
            };
        };
    },

    addCommands() {
        return {
            insertGrid:
                () =>
                ({ commands, state }) => {
                    // Prevent inserting a grid inside a grid column.
                    const { $from } = state.selection;

                    for (let d = $from.depth; d > 0; d--) {
                        if ($from.node(d).type.name === 'gridColumn') return false;
                    }

                    return commands.insertContent({
                        type: 'gridBlock',
                        attrs: { columns: [6, 6] },
                        content: [
                            { type: 'gridColumn', content: [{ type: 'paragraph' }] },
                            { type: 'gridColumn', content: [{ type: 'paragraph' }] }
                        ]
                    });
                },

            setGridColumns:
                (columns: number[]) =>
                ({ tr, state, dispatch }) => {
                    const { $from } = state.selection;

                    for (let depth = $from.depth; depth > 0; depth--) {
                        if ($from.node(depth).type.name === 'gridBlock') {
                            const pos = $from.before(depth);

                            if (dispatch) {
                                tr.setNodeMarkup(pos, undefined, {
                                    ...$from.node(depth).attrs,
                                    columns
                                });
                            }

                            return true;
                        }
                    }

                    return false;
                }
        };
    },

    addKeyboardShortcuts() {
        return {
            Backspace: ({ editor }) => {
                const { state } = editor;
                const { $from } = state.selection;

                for (let depth = $from.depth; depth > 0; depth--) {
                    if ($from.node(depth).type.name === 'gridBlock') {
                        const gridNode = $from.node(depth);
                        const gridPos = $from.before(depth);

                        const bothEmpty =
                            gridNode.childCount === 2 &&
                            gridNode.child(0).childCount === 1 &&
                            gridNode.child(0).child(0).type.name === 'paragraph' &&
                            gridNode.child(0).child(0).textContent === '' &&
                            gridNode.child(1).childCount === 1 &&
                            gridNode.child(1).child(0).type.name === 'paragraph' &&
                            gridNode.child(1).child(0).textContent === '';

                        if (bothEmpty) {
                            const { tr } = state;
                            const paragraph = state.schema.nodes['paragraph'].create();
                            tr.replaceWith(gridPos, gridPos + gridNode.nodeSize, paragraph);
                            tr.setSelection(TextSelection.near(tr.doc.resolve(gridPos + 1)));
                            editor.view.dispatch(tr);

                            return true;
                        }

                        return false;
                    }
                }

                return false;
            },

            Delete: ({ editor }) => {
                const { state } = editor;
                const { $from, empty } = state.selection;

                if (!empty) return false;

                for (let depth = $from.depth; depth > 0; depth--) {
                    if ($from.node(depth).type.name === 'gridColumn') {
                        const cursorAtEnd = $from.pos === $from.end($from.depth);
                        const lastChildInColumn =
                            $from.index(depth) === $from.node(depth).childCount - 1;

                        if (cursorAtEnd && lastChildInColumn) return true;

                        return false;
                    }
                }

                return false;
            },

            Tab: ({ editor }) => {
                const { state } = editor;
                const { $from } = state.selection;

                for (let depth = $from.depth; depth > 0; depth--) {
                    if ($from.node(depth).type.name === 'gridBlock') {
                        const gridNode = $from.node(depth);
                        const gridPos = $from.before(depth);

                        let columnIndex = -1;

                        for (let colDepth = $from.depth; colDepth > depth; colDepth--) {
                            if ($from.node(colDepth).type.name === 'gridColumn') {
                                const colPos = $from.before(colDepth);
                                let offset = 0;

                                for (let i = 0; i < gridNode.childCount; i++) {
                                    if (gridPos + 1 + offset === colPos) {
                                        columnIndex = i;
                                        break;
                                    }

                                    offset += gridNode.child(i).nodeSize;
                                }

                                break;
                            }
                        }

                        if (columnIndex === 0 && gridNode.childCount > 1) {
                            const firstColSize = gridNode.child(0).nodeSize;
                            const secondColStart = gridPos + 1 + firstColSize + 1;
                            const resolvedPos = state.doc.resolve(secondColStart);
                            editor.commands.setTextSelection(TextSelection.near(resolvedPos).from);

                            return true;
                        }

                        return false;
                    }
                }

                return false;
            },

            'Shift-Tab': ({ editor }) => {
                const { state } = editor;
                const { $from } = state.selection;

                for (let depth = $from.depth; depth > 0; depth--) {
                    if ($from.node(depth).type.name === 'gridBlock') {
                        const gridNode = $from.node(depth);
                        const gridPos = $from.before(depth);

                        let columnIndex = -1;

                        for (let colDepth = $from.depth; colDepth > depth; colDepth--) {
                            if ($from.node(colDepth).type.name === 'gridColumn') {
                                const colPos = $from.before(colDepth);
                                let offset = 0;

                                for (let i = 0; i < gridNode.childCount; i++) {
                                    if (gridPos + 1 + offset === colPos) {
                                        columnIndex = i;
                                        break;
                                    }

                                    offset += gridNode.child(i).nodeSize;
                                }

                                break;
                            }
                        }

                        if (columnIndex === 1) {
                            const firstColStart = gridPos + 1 + 1;
                            const resolvedPos = state.doc.resolve(firstColStart);
                            editor.commands.setTextSelection(TextSelection.near(resolvedPos).from);

                            return true;
                        }

                        return false;
                    }
                }

                return false;
            }
        };
    },

    addProseMirrorPlugins() {
        return [GridResizePlugin(this.editor)];
    }
});
