import { Plugin, PluginKey, EditorState, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Subject } from 'rxjs';
import tippy, { Instance, Props } from 'tippy.js';

import { ComponentRef } from '@angular/core';

import { takeUntil } from 'rxjs/operators';

import { Editor } from '@tiptap/core';
import { BubbleMenuView } from '@tiptap/extension-bubble-menu';

import { AIContentPromptComponent } from '../ai-content-prompt.component';
import { AI_CONTENT_PROMPT_PLUGIN_KEY } from '../ai-content-prompt.extension';
import { TIPPY_OPTIONS } from '../utils';

interface AIContentPromptProps {
    pluginKey: PluginKey;
    editor: Editor;
    element: HTMLElement;
    tippyOptions?: Partial<Props>;
    component: ComponentRef<AIContentPromptComponent>;
}

interface PluginState {
    open: boolean;
    form: [];
}

export type AIContentPromptViewProps = AIContentPromptProps & {
    view: EditorView;
};

export class AIContentPromptView extends BubbleMenuView {
    public editor: Editor;

    public element: HTMLElement;

    public view: EditorView;

    public tippy: Instance | undefined;

    public tippyOptions?: Partial<Props>;

    public pluginKey: PluginKey;

    public component?: ComponentRef<AIContentPromptComponent>;

    private $destroy = new Subject<boolean>();

    constructor(props: AIContentPromptViewProps) {
        const { editor, element, view, tippyOptions = {}, pluginKey, component } = props;

        super(props);

        this.editor = editor;
        this.element = element;
        this.view = view;

        this.tippyOptions = tippyOptions;

        this.element.remove();
        this.element.style.visibility = 'visible';
        this.pluginKey = pluginKey;
        this.component = component;

        this.component.instance.buildForm();

        this.component.instance.formValues.pipe(takeUntil(this.$destroy)).subscribe((data) => {
            this.editor.commands.updateValue(data);
        });

        this.component.instance.hide.pipe(takeUntil(this.$destroy)).subscribe(() => {
            this.editor.commands.closeAIPrompt();
        });

        this.editor.on('focus', this.focusHandler);
    }

    update(view: EditorView, prevState?: EditorState) {
        const next = this.pluginKey?.getState(view.state);
        const prev = prevState ? this.pluginKey?.getState(prevState) : { open: false };

        if (next?.open === prev?.open) {
            this.tippy?.popperInstance?.forceUpdate();

            return;
        }

        if (next.open && next.form) {
            this.component.instance.buildForm();
        } else {
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

        this.tippy = tippy(editorElement.parentElement, {
            ...this.tippyOptions,
            ...TIPPY_OPTIONS,
            content: this.element,
            onShow: () => {
                requestAnimationFrame(() => {
                    this.component.instance.input.nativeElement.focus();
                });
            }
        });
    }

    focusHandler = () => {
        this.editor.commands.closeForm();
        setTimeout(() => this.update(this.editor.view));
    };

    show() {
        this.tippy?.show();
    }

    hide() {
        this.tippy?.hide();
        this.editor.view.focus();
    }

    destroy() {
        this.tippy?.destroy();
        this.editor.off('focus', this.focusHandler);
        this.$destroy.next(true);
        this.component.destroy();
        this.component.instance.formValues.unsubscribe();
    }

    private hanlderScroll(e: Event) {
        if (!this.shouldHideOnScroll(e.target as HTMLElement)) {
            return true;
        }

        // we use `setTimeout` to make sure `selection` is already updated
        setTimeout(() => this.update(this.editor.view));
    }

    private shouldHideOnScroll(node: HTMLElement): boolean {
        return this.tippy?.state.isMounted && this.tippy?.popper.contains(node);
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
                const state = AI_CONTENT_PROMPT_PLUGIN_KEY?.getState(oldState);

                if (typeof open === 'boolean') {
                    return { open, form };
                }

                // keep the old state in case we do not receive a new one.
                return state || value;
            }
        }
    });
};
