import { Node } from 'prosemirror-model';
import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Subject } from 'rxjs';
import tippy, { Instance, Props } from 'tippy.js';

import { ComponentRef } from '@angular/core';

import { Editor } from '@tiptap/core';

import { AiContentPromptStore } from '../../ai-content-prompt/store/ai-content-prompt.store';
import { DotAiImagePromptStore } from '../../ai-image-prompt/ai-image-prompt.store';
import { AIContentActionsComponent } from '../ai-content-actions.component';
import { AI_CONTENT_ACTIONS_PLUGIN_KEY } from '../ai-content-actions.extension';
import { TIPPY_OPTIONS } from '../utils';

interface AIContentActionsProps {
    pluginKey: PluginKey;
    editor: Editor;
    element: HTMLElement;
    tippyOptions?: Partial<Props>;
    component: ComponentRef<AIContentActionsComponent>;
}

interface PluginState {
    open: boolean;
    nodeType: string;
}

export type AIContentActionsViewProps = AIContentActionsProps & {
    view: EditorView;
};

export class AIContentActionsView {
    public editor: Editor;

    public node: Node;

    public element: HTMLElement;

    public view: EditorView;

    public tippy: Instance | undefined;

    public tippyOptions?: Partial<Props>;

    public pluginKey: PluginKey;

    public component: ComponentRef<AIContentActionsComponent>;

    private aiContentPromptStore: AiContentPromptStore;
    private dotAiImagePromptStore: DotAiImagePromptStore;

    private destroy$ = new Subject<boolean>();

    constructor(props: AIContentActionsViewProps) {
        const { editor, element, view, tippyOptions = {}, pluginKey, component } = props;

        this.editor = editor;
        this.element = element;
        this.view = view;

        this.tippyOptions = tippyOptions;

        this.element.remove();
        this.pluginKey = pluginKey;
        this.component = component;

        // Reference of stores available ROOT through the Angular component.
        this.aiContentPromptStore = this.component.injector.get(AiContentPromptStore);
        this.dotAiImagePromptStore = this.component.injector.get(DotAiImagePromptStore);

        this.view.dom.addEventListener('keydown', this.handleKeyDown.bind(this));
    }

    update(view: EditorView, prevState?: EditorState) {
        const next = this.pluginKey?.getState(view.state);
        const prev = prevState ? this.pluginKey?.getState(prevState) : { open: false };

        if (next?.open === prev?.open) {
            this.tippy?.popperInstance?.forceUpdate();

            return;
        }

        this.createTooltip();

        next.open ? this.show() : this.hide();
    }

    createTooltip() {
        const { element: editorElement } = this.editor.options;
        const editorIsAttached = !!editorElement.parentElement;

        if (this.tippy || !editorIsAttached) {
            return;
        }

        this.tippy = tippy(editorElement.parentElement, {
            ...TIPPY_OPTIONS,
            ...this.tippyOptions,
            content: this.element
        });
    }

    show() {
        this.tippy?.show();
    }

    hide() {
        this.tippy?.hide();
        this.editor.view.focus();
    }

    destroy() {
        this.tippy?.destroy();
        this.destroy$.next(true);
        this.destroy$.complete();
        this.view.dom.removeEventListener('keydown', this.handleKeyDown);
    }

    private acceptContent() {
        this.editor.commands.closeAIContentActions();
    }

    private generateContent() {
        this.editor.commands.closeAIContentActions();
    }

    private handleKeyDown(event: KeyboardEvent) {
        if (event.key === 'Backspace') {
            this.editor.commands.closeAIContentActions();
        }
    }
}

export const aiContentActionsPlugin = (options: AIContentActionsProps) => {
    return new Plugin({
        key: options.pluginKey as PluginKey,
        view: (view) => new AIContentActionsView({ view, ...options }),
        state: {
            init(): PluginState {
                return {
                    open: false,
                    nodeType: null
                };
            },

            apply(
                transaction: Transaction,
                value: PluginState,
                oldState: EditorState
            ): PluginState {
                const { open, nodeType } = transaction.getMeta(AI_CONTENT_ACTIONS_PLUGIN_KEY) || {};
                const state = AI_CONTENT_ACTIONS_PLUGIN_KEY?.getState(oldState);

                if (typeof open === 'boolean') {
                    return { open, nodeType };
                }

                return state || value;
            }
        }
    });
};
