import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { Editor, posToDOMRect, Range } from '@tiptap/core';

import { SuggestionsCommandProps } from '../components/suggestions/suggestions.component';

interface PluginState {
    open: boolean;
    range?: Range;
}

export interface FloatingActionsPluginProps {
    editor: Editor;
    render?: () => FloatingRenderActions;
    command: ({
        editor,
        range,
        props
    }: {
        editor: Editor;
        range: Range;
        props: SuggestionsCommandProps;
    }) => void;
}

export type FloatingActionsViewProps = FloatingActionsPluginProps & {
    view: EditorView;
    key: PluginKey;
};

export interface FloatingActionsProps {
    range: Range;
    editor: Editor;
    command: (props: { editor: Editor; range: Range; props: SuggestionsCommandProps }) => void;
    clientRect: (() => DOMRect) | null;
}

export interface FloatingActionsKeydownProps {
    view: EditorView;
    event: KeyboardEvent;
    range: Range;
}

export interface FloatingRenderActions {
    onStart?: (props: FloatingActionsProps) => void;
    onExit?: (props: FloatingActionsProps) => void;
    onKeyDown?: (props: FloatingActionsKeydownProps) => boolean;
}

export const FLOATING_ACTIONS_MENU_KEYBOARD = 'menuFloating';

export class FloatingActionsView {
    editor: Editor;
    element: HTMLElement;
    view: EditorView;
    render: () => FloatingRenderActions;
    command: (props: { editor: Editor; range: Range; props: SuggestionsCommandProps }) => void;
    key: PluginKey;
    invalidNodes = ['codeBlock', 'blockquote'];

    constructor({ editor, view, render, command, key }: FloatingActionsViewProps) {
        this.editor = editor;
        this.view = view;
        this.editor.on('focus', () => {
            this.update(this.editor.view);
        });
        this.render = render;
        this.command = command;
        this.key = key;
    }

    /**
     * Check the EditorState and based on that modify the DOM
     *
     * @param {EditorView} view
     * @param {EditorState} prevState
     * @return {*}  {void}
     * @memberof FloatingActionsView
     */
    update(view: EditorView, prevState?: EditorState): void {
        const next = this.key?.getState(view.state);
        const prev = prevState ? this.key?.getState(prevState) : null;

        if (next.open) {
            const { from, to } = this.editor.state.selection;
            const rect = posToDOMRect(this.view, from, to);

            this.render().onStart({
                clientRect: () => rect,
                range: { from, to },
                editor: this.editor,
                command: this.command
            });
        } else if (prev && prev.open) {
            this.render().onExit(null);
        }
    }
}

export const FloatingActionsPluginKey = new PluginKey(FLOATING_ACTIONS_MENU_KEYBOARD);

export const FloatingActionsPlugin = (options: FloatingActionsPluginProps) => {
    return new Plugin({
        key: FloatingActionsPluginKey,
        view: (view) =>
            new FloatingActionsView({ key: FloatingActionsPluginKey, view, ...options }),
        state: {
            /**
             * Init the plugin state
             *
             * @return {*}  {PluginState}
             */
            init(): PluginState {
                return {
                    open: false
                };
            },
            /**
             * Update the plugin state base on meta information
             *
             * @param {Transaction} transaction
             * @return {*}  {PluginState}
             */
            apply(transaction: Transaction): PluginState {
                const transactionMeta = transaction.getMeta(FLOATING_ACTIONS_MENU_KEYBOARD);

                if (transactionMeta?.open) {
                    return {
                        open: transactionMeta?.open
                    };
                }

                return {
                    open: false
                };
            }
        },
        props: {
            /**
             * Catch and handle the keydown in the plugin
             *
             * @param {EditorView} view
             * @param {KeyboardEvent} event
             * @return {*}
             */
            handleKeyDown(view: EditorView, event: KeyboardEvent) {
                const { open, range } = this.getState(view.state);
                if (!open) {
                    return false;
                }

                return options.render().onKeyDown({ event, range, view });
            }
        }
    });
};
