import { EditorView } from 'prosemirror-view';
import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { Editor, posToDOMRect } from '@tiptap/core';

import { RenderProps } from '../image-tabview-form.extension';

interface PluginState {
    open: boolean;
}

export interface BubbleImageTabFormProps {
    pluginKey: PluginKey;
    editor: Editor;
    render?: () => RenderProps;
}

export type BubbleImageTabFormViewProps = BubbleImageTabFormProps & {
    view: EditorView;
};

export class BubbleImageTabFormView {
    private editor: Editor;
    private view: EditorView;
    private pluginKey: PluginKey;
    private render: () => RenderProps;

    constructor({ editor, view, pluginKey, render }: BubbleImageTabFormViewProps) {
        this.editor = editor;
        this.view = view;
        this.pluginKey = pluginKey;
        this.render = render;
        this.editor.on('focus', () => this.render().onHide(this.editor));
    }

    update(view: EditorView, prevState?: EditorState): void {
        const next = this.pluginKey?.getState(view.state);
        const prev = prevState ? this.pluginKey?.getState(prevState) : { open: false };

        const { state } = view;
        const { selection } = state;

        if (next?.open === prev?.open) {
            return;
        }

        if (!next?.open) {
            this.render().onHide(this.editor);

            return;
        } else {
            this.render().onStart({
                editor: this.editor,
                getPosition: () => {
                    const { from, to } = selection;

                    return posToDOMRect(view, from, to);
                }
            });
        }
    }

    destroy() {
        this.render().onDestroy();
    }
}

export const bubbleImageTabviewFormPlugin = (options: BubbleImageTabFormProps) => {
    return new Plugin({
        key: options.pluginKey as PluginKey,
        view: (view) => new BubbleImageTabFormView({ view, ...options }),
        state: {
            init(): PluginState {
                return {
                    open: false
                };
            },

            apply(
                transaction: Transaction,
                value: PluginState,
                oldState: EditorState
            ): PluginState {
                const { open } = transaction.getMeta(options.pluginKey) || {};
                const state = options.pluginKey?.getState(oldState);

                if (typeof open === 'boolean') {
                    return { open };
                }

                // keep the old state in case we do not receive a new one.
                return state || value;
            }
        }
    });
};
