import { Node } from 'prosemirror-model';
import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Subject } from 'rxjs';
import { Instance, Props } from 'tippy.js';

import { ComponentRef } from '@angular/core';

import { filter, skip, takeUntil } from 'rxjs/operators';

import { Editor } from '@tiptap/core';

import { DotAIImagePromptComponent, DotAiImagePromptStore } from '@dotcms/ui';

import { AI_IMAGE_PROMPT_PLUGIN_KEY } from './ai-image-prompt.extension';

interface AIImagePromptProps {
    pluginKey: PluginKey;
    editor: Editor;
    element: HTMLElement;
    component: ComponentRef<DotAIImagePromptComponent>;
}

interface PluginState {
    aIImagePromptOpen: boolean;
}

export type AIImagePromptViewProps = AIImagePromptProps & {
    view: EditorView;
};

export class AIImagePromptView {
    public editor: Editor;

    public node: Node;

    public element: HTMLElement;

    public view: EditorView;

    public tippy: Instance | undefined;

    public tippyOptions: Partial<Props>;

    public pluginKey: PluginKey;

    public component: ComponentRef<DotAIImagePromptComponent>;

    private destroy$ = new Subject<boolean>();

    private store: DotAiImagePromptStore;

    /**
     * Creates a new instance of the AIImagePromptView class.
     * @param {AIImagePromptViewProps} props - The properties for the component.
     */
    constructor(props: AIImagePromptViewProps) {
        const { editor, element, view, pluginKey, component } = props;

        this.editor = editor;
        this.element = element;
        this.view = view;

        this.element.remove();
        this.pluginKey = pluginKey;
        this.component = component;

        this.store = this.component.injector.get(DotAiImagePromptStore);

        /**
         * Subscription fired by the store when the dialog change of the state
         * Handle the manual close of the dialog (esc, click outside, x button)
         */
        this.store.isOpenDialog$
            .pipe(
                skip(1),
                filter((value) => value === false),
                takeUntil(this.destroy$)
            )
            .subscribe(() => {
                this.editor.commands.closeImagePrompt();
            });

        /**
         * Subscription fired by the store when image is seleted
         * from the gallery to be inserted it into the editor
         */
        this.store.selectedImage$
            .pipe(
                filter((selectedImage) => !!selectedImage),
                takeUntil(this.destroy$)
            )
            .subscribe((selectedImage) => {
                this.editor.chain().insertImage(selectedImage.response.contentlet).run();
                // A new image is being inserted
                this.store.hideDialog();
                this.editor.chain().closeImagePrompt().run();
            });
    }

    update(view: EditorView, prevState: EditorState) {
        const next = this.pluginKey?.getState(view.state);
        const prev = prevState ? this.pluginKey?.getState(prevState) : { aIImagePromptOpen: false };

        // show the dialog
        if (next.aIImagePromptOpen && prev.aIImagePromptOpen === false) {
            this.store.showDialog(this.editor.getText());
        }

        // hide the dialog handled by isOpenDialog$ subscription
    }

    destroy() {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}

export const aiImagePromptPlugin = (options: AIImagePromptProps) => {
    return new Plugin({
        key: options.pluginKey as PluginKey,
        view: (view) => new AIImagePromptView({ view, ...options }),
        state: {
            init(): PluginState {
                return {
                    aIImagePromptOpen: false
                };
            },

            apply(
                transaction: Transaction,
                value: PluginState,
                oldState: EditorState
            ): PluginState {
                const { aIImagePromptOpen } = transaction.getMeta(AI_IMAGE_PROMPT_PLUGIN_KEY) || {};
                const state = AI_IMAGE_PROMPT_PLUGIN_KEY.getState(oldState);
                if (typeof aIImagePromptOpen === 'boolean') {
                    return { aIImagePromptOpen };
                }

                return state || value;
            }
        }
    });
};
