import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { Editor, posToDOMRect } from '@tiptap/core';

import { RenderProps } from '../asset-tabview-form.extension';

interface PluginState {
    open: boolean;
    asset: string;
}

export interface BubbleAssetTabFormProps {
    pluginKey: PluginKey;
    editor: Editor;
    render?: () => RenderProps;
}

export type BubbleAssetTabFormViewProps = BubbleAssetTabFormProps & {
    view: EditorView;
};

export class BubbleAssetTabFormView {
    private editor: Editor;
    private view: EditorView;
    private pluginKey: PluginKey;
    private render: () => RenderProps;

    constructor({ editor, view, pluginKey, render }: BubbleAssetTabFormViewProps) {
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

        // eslint-disable-next-line no-console
        if (next?.open === prev?.open) {
            return;
        }

        if (!next?.open) {
            this.render().onHide(this.editor);

            return;
        } else {
            this.render().onStart({
                editor: this.editor,
                asset: next?.asset,
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

export const bubbleAssetTabviewFormPlugin = (options: BubbleAssetTabFormProps) => {
    return new Plugin({
        key: options.pluginKey as PluginKey,
        view: (view) => new BubbleAssetTabFormView({ view, ...options }),
        state: {
            init(): PluginState {
                return {
                    open: false,
                    asset: ''
                };
            },

            apply(
                transaction: Transaction,
                value: PluginState,
                oldState: EditorState
            ): PluginState {
                const { open, asset } = transaction.getMeta(options.pluginKey) || {};
                const state = options.pluginKey?.getState(oldState);

                if (typeof open === 'boolean') {
                    return { open, asset };
                }

                // keep the old state in case we do not receive a new one.
                return state || value;
            }
        }
    });
};
