import { EditorView } from 'prosemirror-view';
import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { Editor, posToDOMRect } from '@tiptap/core';

import { EditorAssetTypes } from '@dotcms/dotcms-models';

import { RenderProps } from '../asset-form.extension';

interface PluginState {
    open: boolean;
    type: EditorAssetTypes;
}

export interface BubbleAssetFormProps {
    pluginKey: PluginKey;
    editor: Editor;
    render?: () => RenderProps;
}

export type BubbleAssetFormViewProps = BubbleAssetFormProps & {
    view: EditorView;
};

export class BubbleAssetFormView {
    private editor: Editor;
    private view: EditorView;
    private pluginKey: PluginKey;
    private render: () => RenderProps;

    constructor({ editor, view, pluginKey, render }: BubbleAssetFormViewProps) {
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
        } else {
            this.render().onStart({
                editor: this.editor,
                type: next.type,
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

export const bubbleAssetFormPlugin = (options: BubbleAssetFormProps) => {
    return new Plugin({
        key: options.pluginKey,
        view: (view) => new BubbleAssetFormView({ view, ...options }),
        state: {
            init(): PluginState {
                return {
                    open: false,
                    type: null
                };
            },

            apply(
                transaction: Transaction,
                value: PluginState,
                oldState: EditorState
            ): PluginState {
                const { open, type } = transaction.getMeta(options.pluginKey) || {};
                const state = options.pluginKey?.getState(oldState);

                if (typeof open === 'boolean') {
                    return { open, type };
                }

                // keep the old state in case we do not receive a new one.
                return state || value;
            }
        }
    });
};
