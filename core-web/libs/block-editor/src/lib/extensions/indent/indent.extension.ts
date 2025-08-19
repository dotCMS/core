import { AllSelection, TextSelection, Transaction } from 'prosemirror-state';

import { Extension } from '@tiptap/core';

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        indent: {
            /**
             * Indent content
             */
            indent: () => ReturnType;
            /**
             * Outdent content
             */
            outdent: () => ReturnType;
        };
    }
}

export interface IndentOptions {
    /**
     * The types of nodes that should be indented
     */
    types: string[];

    /**
     * The minimum indentation level
     */
    minIndentLevel: number;

    /**
     * The maximum indentation level
     */
    maxIndentLevel: number;

    /**
     * The indent size
     */
    indentSize: number;
}

function clamp(val: number, min: number, max: number): number {
    if (val < min) {
        return min;
    }

    if (val > max) {
        return max;
    }

    return val;
}

const INDENT_MIN = 0;
const INDENT_MAX = 400; // Maximum limit of 400px
const INDENT_MORE = 40; // Increment of 40px
const INDENT_LESS = -40;

function setNodeIndentMarkup(tr: Transaction, pos: number, delta: number): Transaction {
    if (!tr.doc) return tr;

    const node = tr.doc.nodeAt(pos);
    if (!node) return tr;

    const minIndent = INDENT_MIN;
    const maxIndent = INDENT_MAX;
    const indent = clamp((node.attrs.indent || 0) + delta, minIndent, maxIndent);

    if (indent === node.attrs.indent) return tr;

    const nodeAttrs = {
        ...node.attrs,
        indent
    };

    return tr.setNodeMarkup(pos, node.type, nodeAttrs, node.marks);
}

function updateIndentLevel(tr: Transaction, delta: number): Transaction {
    const { doc, selection } = tr;

    if (!doc || !selection) return tr;

    if (!(selection instanceof TextSelection || selection instanceof AllSelection)) {
        return tr;
    }

    const { from, to } = selection;

    doc.nodesBetween(from, to, (node, pos) => {
        const nodeType = node.type;

        // Only handle paragraphs, headings and blockquotes - NO lists
        if (
            nodeType.name === 'paragraph' ||
            nodeType.name === 'heading' ||
            nodeType.name === 'blockquote'
        ) {
            tr = setNodeIndentMarkup(tr, pos, delta);

            return false;
        }

        return true;
    });

    return tr;
}

export const IndentExtension = Extension.create<IndentOptions>({
    name: 'indent',

    addOptions() {
        return {
            types: ['heading', 'paragraph', 'blockquote'],
            minIndentLevel: 0,
            maxIndentLevel: 10,
            indentSize: 40
        };
    },

    addGlobalAttributes() {
        return [
            {
                types: this.options.types,
                attributes: {
                    indent: {
                        default: 0,
                        renderHTML: (attributes) => {
                            if (!attributes.indent || attributes.indent <= 0) {
                                return {};
                            }

                            return {
                                style: `margin-left: ${attributes.indent}px;`
                            };
                        },
                        parseHTML: (element) => {
                            const marginLeft = element.style.marginLeft;
                            if (!marginLeft) return 0;

                            const value = parseInt(marginLeft, 10);

                            return value
                                ? Math.round(value / this.options.indentSize) *
                                      this.options.indentSize
                                : 0;
                        }
                    }
                }
            }
        ];
    },

    addCommands() {
        return {
            indent:
                () =>
                ({ state, dispatch }) => {
                    // Only for paragraphs, headings, blockquotes - NO lists
                    const { selection } = state;
                    const newTr = state.tr.setSelection(selection);
                    const updatedTr = updateIndentLevel(newTr, INDENT_MORE);

                    if (updatedTr.docChanged && dispatch) {
                        dispatch(updatedTr);

                        return true;
                    }

                    return false;
                },

            outdent:
                () =>
                ({ state, dispatch }) => {
                    // Only for paragraphs, headings, blockquotes - NO lists
                    const { selection } = state;
                    const newTr = state.tr.setSelection(selection);
                    const updatedTr = updateIndentLevel(newTr, INDENT_LESS);

                    if (updatedTr.docChanged && dispatch) {
                        dispatch(updatedTr);

                        return true;
                    }

                    return false;
                }
        };
    },

    addKeyboardShortcuts() {
        return {
            Tab: () => {
                // Don't handle Tab if we're in a list
                if (this.editor.isActive('bulletList') || this.editor.isActive('orderedList')) {
                    return false;
                }

                return this.editor.commands.indent();
            },
            'Shift-Tab': () => {
                // Don't handle Shift-Tab if we're in a list
                if (this.editor.isActive('bulletList') || this.editor.isActive('orderedList')) {
                    return false;
                }

                return this.editor.commands.outdent();
            },
            Backspace: () => {
                const { state } = this.editor;
                const { from, to } = state.selection;

                // Only outdent if we're at the beginning of the node without selection
                const hasTextBeforeCursor =
                    from > 1 && state.doc.textBetween(from - 1, from).trim().length > 0;
                const hasSelection = from !== to;

                if (hasTextBeforeCursor || hasSelection) {
                    return false;
                }

                // Don't handle if we're in a list
                if (this.editor.isActive('bulletList') || this.editor.isActive('orderedList')) {
                    return false;
                }

                return this.editor.commands.outdent();
            }
        };
    }
});
