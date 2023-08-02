import { Plugin, PluginKey, EditorState } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Observable, Subject } from 'rxjs';
import tippy, { Instance, Props } from 'tippy.js';

import { ComponentRef } from '@angular/core';

import { Editor } from '@tiptap/core';

import { AIContentPromptComponent } from '../ai-content-prompt.component';

const TIPPY_OPTIONS: Partial<Props> = {
    duration: [250, 0],
    interactive: true,
    maxWidth: '100%',
    trigger: 'manual',
    hideOnClick: 'toggle',
    popperOptions: {
        modifiers: [
            {
                name: 'flip',
                options: { fallbackPlacements: ['bottom'] }
            },
            {
                name: 'preventOverflow',
                options: {
                    padding: { left: 10, right: 10 },
                    boundary: 'viewport'
                }
            }
        ]
    }
};

interface AIContentPromptProps {
    pluginKey: PluginKey;
    editor: Editor;
    element: HTMLElement;
    tippyOptions?: Partial<Props>;
    component: ComponentRef<AIContentPromptComponent>;
    form$: Observable<{ [key: string]: string }>;
}

export type AIContentPromptViewProps = AIContentPromptProps & {
    view: EditorView;
};

export class AIContentPromptView {
    public editor: Editor;

    public element: HTMLElement;

    public view: EditorView;

    public tippy: Instance | undefined;

    public tippyOptions?: Partial<Props>;

    public pluginKey: PluginKey;

    public component?: ComponentRef<AIContentPromptComponent>;

    public destroy$ = new Subject<boolean>();

    constructor(props: AIContentPromptViewProps) {
        const { editor, element, view, tippyOptions = {}, pluginKey, component } = props;

        this.editor = editor;
        this.element = element;
        this.view = view;

        this.tippyOptions = tippyOptions;

        this.element.remove();
        this.element.style.visibility = 'visible';
        this.pluginKey = pluginKey;
        this.component = component;

        this.component.instance.buildForm();
    }

    update(view: EditorView, prevState: EditorState) {
        const next = this.pluginKey?.getState(view.state);
        const prev = prevState ? this.pluginKey?.getState(prevState) : { open: false };

        if (next?.open === prev?.open) {
            this.tippy?.popperInstance?.forceUpdate();

            return;
        }

        if (next.open && next.form) {
            this.component.instance.buildForm();
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

    show() {
        this.tippy?.show();
    }

    hide() {
        this.tippy?.hide();
        this.editor.view.focus();
    }

    destroy() {
        this.destroy$.next(true);
        this.destroy$.complete();
        this.tippy?.destroy();
    }
}

export const aiContentPromptPlugin = (options: AIContentPromptProps) => {
    return new Plugin({
        key: options.pluginKey,
        view: (view) => new AIContentPromptView({ view, ...options }),
        state: {
            init: () => ({ open: false }),
            apply: (tr, prevState) => {
                const next = tr.getMeta(options.pluginKey);
                if (next && next.open !== prevState.open) {
                    return { open: next.open };
                }

                return prevState;
            }
        }
    });
};
