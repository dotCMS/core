import { Node, mergeAttributes } from '@tiptap/core';
import { TextSelection } from '@tiptap/pm/state';

import { GridResizePlugin } from './grid-resize.plugin';

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        gridBlock: {
            insertGridBlock: () => ReturnType;
            setGridColumns: (columns: number[]) => ReturnType;
        };
    }
}

export const GridBlock = Node.create({
    name: 'gridBlock',
    group: 'block',
    content: 'gridColumn gridColumn',
    draggable: true,
    isolating: true,

    addAttributes() {
        return {
            columns: {
                default: [6, 6],
                parseHTML: (element: HTMLElement) => {
                    const raw = element.getAttribute('data-columns');

                    try {
                        const parsed = JSON.parse(raw);

                        if (Array.isArray(parsed) && parsed.length === 2) {
                            return parsed;
                        }
                    } catch {
                        // ignore
                    }

                    return [6, 6];
                },
                renderHTML: (attributes: Record<string, unknown>) => {
                    const cols = (attributes.columns as number[]) || [6, 6];
                    const pct1 = (cols[0] / 12) * 100;
                    const pct2 = (cols[1] / 12) * 100;

                    return {
                        'data-columns': JSON.stringify(cols),
                        style: `--col-1: ${pct1}%; --col-2: ${pct2}%`
                    };
                }
            }
        };
    },

    parseHTML() {
        return [{ tag: 'div[data-type="gridBlock"]' }];
    },

    renderHTML({ HTMLAttributes }) {
        return [
            'div',
            mergeAttributes(HTMLAttributes, { 'data-type': 'gridBlock', class: 'grid-block' }),
            0
        ];
    },

    addCommands() {
        return {
            insertGridBlock:
                () =>
                ({ tr, dispatch, editor }) => {
                    const { schema } = editor;
                    const gridColumn = schema.nodes.gridColumn;
                    const paragraph = schema.nodes.paragraph;

                    const col1 = gridColumn.create(null, paragraph.create());
                    const col2 = gridColumn.create(null, paragraph.create());
                    const gridBlock = schema.nodes.gridBlock.create(null, [col1, col2]);

                    if (dispatch) {
                        const pos = tr.selection.from;
                        tr.replaceSelectionWith(gridBlock);
                        // Place cursor in the first column's paragraph
                        const resolvedPos = tr.doc.resolve(pos + 2);
                        tr.setSelection(TextSelection.near(resolvedPos));
                    }

                    return true;
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

                // Find the gridBlock ancestor
                for (let depth = $from.depth; depth > 0; depth--) {
                    if ($from.node(depth).type.name === 'gridBlock') {
                        const gridNode = $from.node(depth);
                        const gridPos = $from.before(depth);

                        // Check if both columns are empty (each has a single empty paragraph)
                        const bothEmpty =
                            gridNode.childCount === 2 &&
                            gridNode.child(0).childCount === 1 &&
                            gridNode.child(0).child(0).type.name === 'paragraph' &&
                            gridNode.child(0).child(0).textContent === '' &&
                            gridNode.child(1).childCount === 1 &&
                            gridNode.child(1).child(0).type.name === 'paragraph' &&
                            gridNode.child(1).child(0).textContent === '';

                        if (bothEmpty) {
                            // Replace the grid block with an empty paragraph
                            const { tr } = state;
                            const from = gridPos;
                            const to = gridPos + gridNode.nodeSize;
                            const paragraph = state.schema.nodes.paragraph.create();
                            tr.replaceWith(from, to, paragraph);
                            tr.setSelection(TextSelection.near(tr.doc.resolve(from + 1)));
                            editor.view.dispatch(tr);

                            return true;
                        }

                        return false;
                    }
                }

                return false;
            },
            Tab: ({ editor }) => {
                const { state } = editor;
                const { $from } = state.selection;

                // Check if we're inside a gridBlock
                for (let depth = $from.depth; depth > 0; depth--) {
                    if ($from.node(depth).type.name === 'gridBlock') {
                        const gridNode = $from.node(depth);
                        const gridPos = $from.before(depth);

                        // Find which column we're in
                        let columnIndex = -1;
                        for (let colDepth = $from.depth; colDepth > depth; colDepth--) {
                            if ($from.node(colDepth).type.name === 'gridColumn') {
                                // Determine column index
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

                        // If in the first column, move to the second
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

                // Check if we're inside a gridBlock
                for (let depth = $from.depth; depth > 0; depth--) {
                    if ($from.node(depth).type.name === 'gridBlock') {
                        const gridNode = $from.node(depth);
                        const gridPos = $from.before(depth);

                        // Find which column we're in
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

                        // If in the second column, move to the first
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
