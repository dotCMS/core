import { Node, DOMSerializer } from 'prosemirror-model';
import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Subject } from 'rxjs';

import { ComponentRef } from '@angular/core';

import { filter, skip, takeUntil } from 'rxjs/operators';

import { Editor } from '@tiptap/core';

import { AIContentPromptComponent } from '../ai-content-prompt.component';
import { AI_CONTENT_PROMPT_PLUGIN_KEY } from '../ai-content-prompt.extension';
import { AiContentPromptStore } from '../store/ai-content-prompt.store';

interface AIContentPromptProps {
    pluginKey: PluginKey;
    editor: Editor;
    element: HTMLElement;
    component: ComponentRef<AIContentPromptComponent>;
}

interface PluginState {
    aIContentPromptOpen: boolean;
}

export type AIContentPromptViewProps = AIContentPromptProps & {
    view: EditorView;
};

/**
 * This class is responsible to create the tippy tooltip and manage the events.
 *
 * The Update method is called when editor(Tiptap) state is updated (to often).
 * then the show() / hide() methods are called if the PluginState property open is true.
 * the others interactions are done by tippy.hide() and tippy.show() methods.
 *  - interaction of the click event in the html template.
 *  - interaction with componentStore.exit$
 *  - Inside the show() method.
 */
export class AIContentPromptView {
    public editor: Editor;

    public node: Node;

    public element: HTMLElement;

    public view: EditorView;

    public pluginKey: PluginKey;

    public component: ComponentRef<AIContentPromptComponent>;

    private componentStore: AiContentPromptStore;

    private destroy$ = new Subject<boolean>();

    constructor(props: AIContentPromptViewProps) {
        const { editor, element, view, pluginKey, component } = props;
        this.editor = editor;
        this.element = element;
        this.view = view;

        this.element.remove();
        this.pluginKey = pluginKey;
        this.component = component;

        this.componentStore = this.component.injector.get(AiContentPromptStore);

        /**
         * Subscription to insert the text Content once accepted in the Dialog.
         * Fired from the AI Content Actions plugin.
         */
        this.componentStore.selectedContent$
            .pipe(takeUntil(this.destroy$), skip(1))
            .subscribe((content) => {
                this.editor.commands.insertContent(this.parseTextToParagraphs(content), {
                    parseOptions: { preserveWhitespace: false }
                });
            });

        /**
         * Subscription to update the editor state, when close the AI Content Prompt Dialog.
         */
        this.componentStore.showDialog$
            .pipe(
                skip(1),
                takeUntil(this.destroy$),
                filter((value) => !value)
            )
            .subscribe(() => {
                this.editor.commands.closeAIPrompt();
            });
    }

    update(view: EditorView, prevState?: EditorState) {
        const next = this.pluginKey?.getState(view.state);
        const prev = prevState
            ? this.pluginKey?.getState(prevState)
            : { aIContentPromptOpen: false };

        if (next?.aIContentPromptOpen && prev?.aIContentPromptOpen === false) {
            this.componentStore.showDialog();
        }
    }

    destroy() {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * This function takes a string of text and converts it into HTML paragraphs.
     * It splits the input text by line breaks and wraps each line in a <p> tag.
     * The resulting HTML string is then returned.
     *
     * based on clipboardTextParser.
     * https://github.com/ProseMirror/prosemirror-view/blob/1.33.4/src/clipboard.ts#L43
     *
     * @param {string} text - The input text to be converted into HTML paragraphs.
     * @returns {string} - The resulting HTML string with text wrapped in <p> tags.
     */
    parseTextToParagraphs(text: string): string {
        const { schema } = this.view.state;
        const serializer = DOMSerializer.fromSchema(schema);
        const dom = document.createElement('div');
        text.split(/(?:\r\n?|\n)+/).forEach((block) => {
            const p = dom?.appendChild(document.createElement('p'));
            if (block) p.appendChild(serializer.serializeNode(schema.text(block)));
        });

        return dom.innerHTML;
    }
}

export const aiContentPromptPlugin = (options: AIContentPromptProps) => {
    return new Plugin({
        key: options.pluginKey as PluginKey,
        view: (view) => new AIContentPromptView({ view, ...options }),
        state: {
            init(): PluginState {
                return {
                    aIContentPromptOpen: false
                };
            },

            apply(
                transaction: Transaction,
                value: PluginState,
                oldState: EditorState
            ): PluginState {
                const { aIContentPromptOpen } =
                    transaction.getMeta(AI_CONTENT_PROMPT_PLUGIN_KEY) || {};
                const state = AI_CONTENT_PROMPT_PLUGIN_KEY.getState(oldState);

                if (typeof aIContentPromptOpen === 'boolean') {
                    return { aIContentPromptOpen };
                }

                // keep the old state in case we do not receive a new one.
                return state || value;
            }
        }
    });
};
