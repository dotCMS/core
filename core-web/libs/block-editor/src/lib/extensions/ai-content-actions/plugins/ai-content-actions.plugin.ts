import { Node } from 'prosemirror-model';
import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Subject } from 'rxjs';
import tippy, { Instance, Props } from 'tippy.js';

import { ComponentRef } from '@angular/core';

import { takeUntil } from 'rxjs/operators';

import { Editor } from '@tiptap/core';

import { ACTIONS, AIContentActionsComponent } from '../ai-content-actions.component';
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

        this.component.instance.actionEmitter.pipe(takeUntil(this.destroy$)).subscribe((action) => {
            switch (action) {
                case ACTIONS.ACCEPT:
                    this.acceptContent();
                    break;

                case ACTIONS.REGENERATE:
                    this.generateContent();
                    break;

                case ACTIONS.DELETE:
                    this.deleteContent();
                    break;
            }
        });

        this.view.dom.addEventListener('keydown', this.handleKeyDown.bind(this));
    }

    private acceptContent() {
        this.editor.commands.closeAIContentActions();
        const content = this.component.instance.getLatestContent();
        this.editor.commands.insertContent(content);
    }

    private generateContent() {
        this.editor.commands.closeAIContentActions();

        this.component.instance
            .getNewContent()
            .pipe(takeUntil(this.destroy$))
            .subscribe((newContent) => {
                if (newContent) {
                    this.editor.commands.deleteSelection();
                    this.editor.commands.insertAINode(newContent);
                    this.editor.commands.openAIContentActions();
                }
            });
    }

    private deleteContent() {
        this.editor.commands.closeAIContentActions();
        this.editor.commands.deleteSelection();
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
}

export const aiContentActionsPlugin = (options: AIContentActionsProps) => {
    return new Plugin({
        key: options.pluginKey as PluginKey,
        view: (view) => new AIContentActionsView({ view, ...options }),
        state: {
            init(): PluginState {
                return {
                    open: false
                };
            },

            apply(
                transaction: Transaction,
                value: PluginState,
                oldState: EditorState
            ): PluginState {
                const { open } = transaction.getMeta(AI_CONTENT_ACTIONS_PLUGIN_KEY) || {};
                const state = AI_CONTENT_ACTIONS_PLUGIN_KEY?.getState(oldState);

                if (typeof open === 'boolean') {
                    return { open };
                }

                return state || value;
            }
        }
    });
};
