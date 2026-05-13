import { AllSelection, TextSelection, type Transaction } from 'prosemirror-state';

import { Extension } from '@tiptap/core';

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        indent: {
            indent: () => ReturnType;
            outdent: () => ReturnType;
        };
    }
}

export interface IndentOptions {
    types: string[];
    indentSize: number;
}

const INDENT_TYPES = ['heading', 'paragraph', 'blockquote'];
const INDENT_MIN = 0;
const INDENT_MAX = 400;
const INDENT_STEP = 40;

function clamp(value: number, min: number, max: number): number {
    return Math.min(Math.max(value, min), max);
}

function setNodeIndentMarkup(tr: Transaction, pos: number, delta: number): Transaction {
    const node = tr.doc?.nodeAt(pos);
    if (!node) return tr;

    const current = (node.attrs['indent'] as number | undefined) ?? 0;
    const next = clamp(current + delta, INDENT_MIN, INDENT_MAX);
    if (next === current) return tr;

    return tr.setNodeMarkup(pos, node.type, { ...node.attrs, indent: next }, node.marks);
}

function updateIndentLevel(tr: Transaction, delta: number): Transaction {
    const { doc, selection } = tr;
    if (!doc || !(selection instanceof TextSelection || selection instanceof AllSelection)) {
        return tr;
    }

    const { from, to } = selection;
    doc.nodesBetween(from, to, (node, pos) => {
        if (INDENT_TYPES.includes(node.type.name)) {
            tr = setNodeIndentMarkup(tr, pos, delta);
            return false;
        }
        return true;
    });

    return tr;
}

/**
 * Per-block indentation for headings, paragraphs and blockquotes.
 *
 * Declares `indent` as a global attribute on the configured node types so existing
 * content authored on the legacy block editor — which stored `attrs.indent` in pixels
 * on these nodes — round-trips through the new editor without losing the value.
 * Tab / Shift-Tab adjust the indent in 40px steps; lists are left to StarterKit so its
 * native list-nesting behavior is preserved.
 */
export const IndentExtension = Extension.create<IndentOptions>({
    name: 'indent',

    addOptions() {
        return {
            types: INDENT_TYPES,
            indentSize: INDENT_STEP
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
                            const value = attributes['indent'] as number | undefined;
                            if (!value || value <= 0) return {};
                            return { style: `margin-left: ${value}px;` };
                        },
                        parseHTML: (element) => {
                            const marginLeft = (element as HTMLElement).style.marginLeft;
                            if (!marginLeft) return 0;
                            const value = parseInt(marginLeft, 10);
                            if (!value) return 0;
                            return (
                                Math.round(value / this.options.indentSize) *
                                this.options.indentSize
                            );
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
                    const tr = updateIndentLevel(
                        state.tr.setSelection(state.selection),
                        INDENT_STEP
                    );
                    if (tr.docChanged && dispatch) {
                        dispatch(tr);
                        return true;
                    }
                    return false;
                },
            outdent:
                () =>
                ({ state, dispatch }) => {
                    const tr = updateIndentLevel(
                        state.tr.setSelection(state.selection),
                        -INDENT_STEP
                    );
                    if (tr.docChanged && dispatch) {
                        dispatch(tr);
                        return true;
                    }
                    return false;
                }
        };
    },

    addKeyboardShortcuts() {
        const inList = () =>
            this.editor.isActive('bulletList') || this.editor.isActive('orderedList');

        return {
            Tab: () => (inList() ? false : this.editor.commands.indent()),
            'Shift-Tab': () => (inList() ? false : this.editor.commands.outdent())
        };
    }
});
