import { Node } from 'prosemirror-model';
import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Subject } from 'rxjs';
import tippy, { Instance, Props } from 'tippy.js';

import { ComponentRef } from '@angular/core';

import { takeUntil } from 'rxjs/operators';

import { Editor } from '@tiptap/core';

import { AIImagePromptComponent } from '../ai-image-prompt.component';
import { AI_IMAGE_PROMPT_PLUGIN_KEY } from '../ai-image-prompt.extension';
import { TIPPY_OPTIONS } from '../utils';

interface AIImagePromptProps {
    pluginKey: PluginKey;
    editor: Editor;
    element: HTMLElement;
    tippyOptions: Partial<Props>;
    component: ComponentRef<AIImagePromptComponent>;
}

interface PluginState {
    open: boolean;
    form: [];
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

    public component: ComponentRef<AIImagePromptComponent>;

    private destroy$ = new Subject<boolean>();

    constructor(props: AIImagePromptViewProps) {
        const { editor, element, view, tippyOptions = {}, pluginKey, component } = props;

        this.editor = editor;
        this.element = element;
        this.view = view;

        this.tippyOptions = tippyOptions;

        this.element.remove();
        this.pluginKey = pluginKey;
        this.component = component;

        this.view.dom.addEventListener('keydown', this.handleKeyDown.bind(this));

        this.component.instance.formSubmission.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.editor.commands.closeImagePrompt();
            this.editor.commands.insertLoaderNode();
        });

        this.component.instance.aiResponse
            .pipe(takeUntil(this.destroy$))
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            .subscribe((contentlet: any) => {
                this.editor.commands.deleteSelection();
                const data = Object.values(contentlet[0])[0];

                if (data) {
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    this.editor.commands.insertImage(data as any);
                    this.editor.commands.openAIContentActions();
                }
            });
    }

    private handleKeyDown(event: KeyboardEvent) {
        if (event.key === 'Backspace') {
            this.editor.commands.closeAIContentActions();
        }
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

        this.tippy = tippy(document.body, {
            ...TIPPY_OPTIONS,
            ...this.tippyOptions,
            content: this.element,
            onHide: () => {
                this.editor.commands.closeImagePrompt();
            }
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
    }
}

export const aiImagePromptPlugin = (options: AIImagePromptProps) => {
    return new Plugin({
        key: options.pluginKey as PluginKey,
        view: (view) => new AIImagePromptView({ view, ...options }),
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
                const { open, form } = transaction.getMeta(AI_IMAGE_PROMPT_PLUGIN_KEY) || {};
                const state = AI_IMAGE_PROMPT_PLUGIN_KEY.getState(oldState);

                if (typeof open === 'boolean') {
                    return { open, form };
                }

                return state || value;
            }
        }
    });
};
