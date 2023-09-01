import { Node } from 'prosemirror-model';
import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Subject } from 'rxjs';
import tippy, { Instance, Props } from 'tippy.js';

import { ComponentRef } from '@angular/core';

import { takeUntil } from 'rxjs/operators';

import { Editor } from '@tiptap/core';

import { AIContentPromptComponent } from '../ai-content-prompt.component';
import { AI_CONTENT_PROMPT_PLUGIN_KEY } from '../ai-content-prompt.extension';
import { TIPPY_OPTIONS } from '../utils';

interface AIContentPromptProps {
    pluginKey: PluginKey;
    editor: Editor;
    element: HTMLElement;
    tippyOptions: Partial<Props>;
    component: ComponentRef<AIContentPromptComponent>;
}

interface PluginState {
    open: boolean;
    form: [];
}

export type AIContentPromptViewProps = AIContentPromptProps & {
    view: EditorView;
};

export class AIContentPromptView {
    public editor: Editor;

    public node: Node;

    public element: HTMLElement;

    public view: EditorView;

    public tippy: Instance | undefined;

    public tippyOptions: Partial<Props>;

    public pluginKey: PluginKey;

    public component: ComponentRef<AIContentPromptComponent>;

    private destroy$ = new Subject<boolean>();

    constructor(props: AIContentPromptViewProps) {
        const { editor, element, view, tippyOptions = {}, pluginKey, component } = props;

        this.editor = editor;
        this.element = element;
        this.view = view;

        this.tippyOptions = tippyOptions;

        this.element.remove();
        this.pluginKey = pluginKey;
        this.component = component;

        this.component.instance.formSubmission.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.editor.commands.closeAIPrompt();
        });

        this.component.instance.aiResponse.pipe(takeUntil(this.$destroy)).subscribe((content) => {
            this.editor.commands.insertAINode(content);
        });
    }

    update(view: EditorView, prevState?: EditorState) {
        const next = this.pluginKey?.getState(view.state);
        const prev = prevState ? this.pluginKey?.getState(prevState) : { open: false };

        if (next?.open === prev?.open) {
            this.tippy?.popperInstance?.forceUpdate();

            return;
        }

        if (!next.open) {
            this.component.instance.cleanForm();
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

        this.tippy = tippy(editorElement, {
            ...TIPPY_OPTIONS,
            ...this.tippyOptions,
            content: this.element,
            onHide: () => {
                this.editor.commands.closeAIPrompt();
            },
            onShow: (instance) => {
                (instance.popper as HTMLElement).style.width = '100%';
            }
        });
    }

    show() {
        this.tippy?.show();
        this.component.instance.focusField();
    }

    hide() {
        this.tippy?.hide();
        this.editor.view.focus();
    }

    destroy() {
        this.tippy?.destroy();
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}

export const aiContentPromptPlugin = (options: AIContentPromptProps) => {
    return new Plugin({
        key: options.pluginKey as PluginKey,
        view: (view) => new AIContentPromptView({ view, ...options }),
        state: {
            init(): PluginState {
                return {
                    open: false,
                    form: []
                };
            },

            apply(
                transaction: Transaction,
                value: PluginState,
                oldState: EditorState
            ): PluginState {
                const { open, form } = transaction.getMeta(AI_CONTENT_PROMPT_PLUGIN_KEY) || {};
                const state = AI_CONTENT_PROMPT_PLUGIN_KEY.getState(oldState);

                if (typeof open === 'boolean') {
                    return { open, form };
                }

                // keep the old state in case we do not receive a new one.
                return state || value;
            }
        }
    });
};
