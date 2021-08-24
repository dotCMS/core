import { Editor, posToDOMRect, Range } from '@tiptap/core';
import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import tippy, { Instance, Props } from 'tippy.js';
import { SuggestionsCommandProps } from '../extensions/components/suggestions/suggestions.component';

interface PluginState {
    open: boolean;
}

export interface FloatingActionsPluginProps {
    editor: Editor;
    element: HTMLElement;
    tippyOptions?: Partial<Props>;
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
    tippy!: Instance;
    render: () => FloatingRenderActions;
    command: (props: { editor: Editor; range: Range; props: SuggestionsCommandProps }) => void;
    key: PluginKey;

    constructor({
        editor,
        element,
        view,
        tippyOptions,
        render,
        command,
        key
    }: FloatingActionsViewProps) {
        this.editor = editor;
        this.element = element;
        this.view = view;
        this.element.addEventListener('mousedown', this.mousedownHandler, { capture: true });
        this.element.style.visibility = 'visible';
        this.render = render;
        this.command = command;
        this.key = key;
        this.createTooltip(tippyOptions);
    }

    /**
     * Element mousedown handler to update the plugin state and open
     *
     * @param {MouseEvent} e
     * @memberof FloatingActionsView
     */
    mousedownHandler = (e: MouseEvent): void => {
        e.preventDefault();

        const transaction = this.editor.state.tr.setMeta(FLOATING_ACTIONS_MENU_KEYBOARD, {
            open: true
        });
        this.editor.view.dispatch(transaction);
    };

    /**
     * Create the tooltip for the element
     *
     * @param {Partial<Props>} [options={}]
     * @memberof FloatingActionsView
     */
    createTooltip(options: Partial<Props> = {}): void {
        this.tippy = tippy(this.view.dom, {
            duration: 0,
            getReferenceClientRect: null,
            content: this.element,
            interactive: true,
            trigger: 'manual',
            placement: 'left',
            hideOnClick: 'toggle',
            ...options
        });
    }

    /**
     * Check the EditorState and based on that modify the DOM
     *
     * @param {EditorView} view
     * @param {EditorState} prevState
     * @return {*}  {void}
     * @memberof FloatingActionsView
     */
    update(view: EditorView, prevState: EditorState): void {
        const { selection } = view.state;
        const { $anchor, empty, from, to } = selection;
        const isRootDepth = $anchor.depth === 1;
        const isNodeEmpty =
            !selection.$anchor.parent.isLeaf && !selection.$anchor.parent.textContent;
        const isActive = isRootDepth && isNodeEmpty;

        if (!empty || !isActive) {
            this.hide();

            return;
        }

        this.tippy.setProps({
            getReferenceClientRect: () => posToDOMRect(view, from, to)
        });

        this.show();

        const next = this.key?.getState(view.state);
        const prev = this.key?.getState(prevState);

        if (next.open) {
            const { from, to } = this.editor.state.selection;
            const rect = posToDOMRect(this.view, from, to);

            this.render().onStart({
                clientRect: () => rect,
                range: { from, to },
                editor: this.editor,
                command: this.command
            });
        } else if (prev.open) {
            this.render().onExit(null);
        }
    }

    show() {
        this.tippy.show();
    }

    hide() {
        this.tippy.hide();
    }

    destroy() {
        this.tippy.destroy();
        this.element.removeEventListener('mousedown', this.mousedownHandler);
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
