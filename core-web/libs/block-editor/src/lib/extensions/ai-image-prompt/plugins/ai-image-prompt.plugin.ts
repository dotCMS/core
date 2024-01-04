import { Node } from 'prosemirror-model';
import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Subject } from 'rxjs';
import { Instance, Props } from 'tippy.js';

import { ComponentRef } from '@angular/core';

import { filter, skip, takeUntil } from 'rxjs/operators';

import { Editor } from '@tiptap/core';

import { findNodeByType, getAIPlaceholderImage } from '../../../shared';
import { NodeTypes } from '../../bubble-menu/models';
import { AIImagePromptComponent } from '../ai-image-prompt.component';
import { AI_IMAGE_PROMPT_PLUGIN_KEY, DOT_AI_IMAGE_CONTENT_KEY } from '../ai-image-prompt.extension';
import { DotAiImagePromptStore } from '../ai-image-prompt.store';

interface AIImagePromptProps {
    pluginKey: PluginKey;
    editor: Editor;
    element: HTMLElement;
    component: ComponentRef<AIImagePromptComponent>;
}

interface PluginState {
    aIImagePromptOpen: boolean;
}

export type AIImagePromptViewProps = AIImagePromptProps & {
    view: EditorView;
};

export const AI_IMAGE_PLACEHOLDER_PROPERTY = 'isAIPlaceholder';

export class AIImagePromptView {
    public editor: Editor;

    public node: Node;

    public element: HTMLElement;

    public view: EditorView;

    public tippy: Instance | undefined;

    public tippyOptions: Partial<Props>;

    public pluginKey: PluginKey;

    public component: ComponentRef<AIImagePromptComponent>;

    private destroy$ = new Subject<boolean>();

    private store: DotAiImagePromptStore;

    constructor(props: AIImagePromptViewProps) {
        const { editor, element, view, pluginKey, component } = props;

        this.editor = editor;
        this.element = element;
        this.view = view;

        this.element.remove();
        this.pluginKey = pluginKey;
        this.component = component;

        this.store = this.component.injector.get(DotAiImagePromptStore);

        // TODO: handle the error

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
         * Subscription fired by the store when the dialog change of the state
         * Handle the click of Generate button
         */
        this.store.isLoading$
            .pipe(
                filter((isLoading) => isLoading === true),
                takeUntil(this.destroy$)
            )
            .subscribe(() => {
                const placeholder = getAIPlaceholderImage(this.editor);

                if (placeholder) {
                    // A regenerate has been requested, so we need to delete the placeholder image
                    this.editor
                        .chain()
                        .deleteRange({
                            from: placeholder.from,
                            to: placeholder.to
                        })
                        .insertLoaderNode(true, placeholder.from)
                        .run();
                } else {
                    // A new image is being inserted
                    this.store.hideDialog();
                    this.editor.chain().insertLoaderNode().closeImagePrompt().run();
                }
            });

        /**
         * Subscription fired by the store when the prompt get a new contentlet to show
         */
        this.store.getContentlets$
            .pipe(
                filter((contentlets) => contentlets.length > 0),
                takeUntil(this.destroy$)
            )
            .subscribe((contentlets) => {
                const data = Object.values(contentlets[0])[0];

                const loaderNodes = findNodeByType(this.editor, NodeTypes.LOADER);

                //Trust in this property to identify the image as a placeholder, until the user accept the content.
                data[AI_IMAGE_PLACEHOLDER_PROPERTY] = true;

                if (loaderNodes) {
                    this.editor
                        .chain()
                        .deleteRange({ from: loaderNodes[0].from, to: loaderNodes[0].to })
                        .insertImage(data, loaderNodes[0].from)
                        .openAIContentActions(DOT_AI_IMAGE_CONTENT_KEY)
                        .run();
                }
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
