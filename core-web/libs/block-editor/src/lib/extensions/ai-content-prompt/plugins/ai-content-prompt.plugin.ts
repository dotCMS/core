import { Node } from 'prosemirror-model';
import { EditorState, Plugin, PluginKey, TextSelection, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Subject } from 'rxjs';
import tippy, { Instance, Props } from 'tippy.js';

import { ComponentRef } from '@angular/core';

import { filter, takeUntil, tap } from 'rxjs/operators';

import { Editor } from '@tiptap/core';

import { findNodeByType, replaceNodeOfTypeWithContent } from '../../../shared';
import { NodeTypes } from '../../bubble-menu/models';
import { AIContentPromptComponent } from '../ai-content-prompt.component';
import {
    AI_CONTENT_PROMPT_PLUGIN_KEY,
    DOT_AI_TEXT_CONTENT_KEY
} from '../ai-content-prompt.extension';
import { AiContentPromptState, AiContentPromptStore } from '../store/ai-content-prompt.store';
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

    private componentStore: AiContentPromptStore;

    private storeSate: AiContentPromptState;

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

        this.componentStore = this.component.injector.get(AiContentPromptStore);

        /**
         * Subscription to insert the AI Node and open the AI Content Actions.
         */
        this.componentStore.content$
            .pipe(
                takeUntil(this.destroy$),
                filter((content) => !!content)
            )
            .subscribe((content) => {
                const { node } = findNodeByType(this.editor, NodeTypes.AI_CONTENT);

                if (node) {
                    replaceNodeOfTypeWithContent(this.editor, NodeTypes.AI_CONTENT, content);
                    this.editor.commands.openAIContentActions(DOT_AI_TEXT_CONTENT_KEY);
                } else {
                    this.editor
                        .chain()
                        .closeAIPrompt()
                        .insertAINode(content)
                        .openAIContentActions(DOT_AI_TEXT_CONTENT_KEY)
                        .run();
                }
            });

        /**
         * Subscription to insert the text Content once accepted the generated content.
         * Fired from the AI Content Actions plugin.
         */
        this.componentStore.vm$
            .pipe(
                takeUntil(this.destroy$),
                tap((state) => (this.storeSate = state)),
                filter((state) => state.acceptContent)
            )
            .subscribe((state) => {
                replaceNodeOfTypeWithContent(this.editor, NodeTypes.AI_CONTENT, state.content);

                this.componentStore.setAcceptContent(false);
            });

        /**
         * Subscription to close the tippy since that can happen on escape listener that is in the html
         * template in ai-content-prompt.component.html
         */
        this.componentStore.open$
            .pipe(
                takeUntil(this.destroy$),
                filter((open) => !open)
            )
            .subscribe(() => this.hide(false));

        this.componentStore.deleteContent$
            .pipe(
                takeUntil(this.destroy$),
                filter((deleteContent) => deleteContent)
            )
            .subscribe(() => {
                replaceNodeOfTypeWithContent(this.editor, NodeTypes.AI_CONTENT, '');
                this.componentStore.setDeleteContent(false);
            });

        this.editor.view.dom.addEventListener('click', this.handleClick.bind(this));
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

        if (!editorIsAttached) {
            return;
        }

        //The following 4 lines are to attach tippy to where the cursor is when opening.
        // Get the current editor selection.
        const { selection } = this.editor.state;
        if (selection instanceof TextSelection) {
            // Use `domAtPos` to get the DOM information at the cursor position
            const { pos } = selection.$cursor;
            const domAtPos = this.editor.view.domAtPos(pos);
            const clientTarget = domAtPos.node as Element;

            this.tippy = tippy(editorElement, {
                ...TIPPY_OPTIONS,
                ...this.tippyOptions,
                content: this.element,
                getReferenceClientRect: clientTarget.getBoundingClientRect.bind(clientTarget),
                onHide: () => {
                    this.editor.commands.closeAIPrompt();
                },
                onShow: (instance) => {
                    const popperElement = instance.popper as HTMLElement;
                    popperElement.style.width = '100%';
                    // override the top position set by popper. so the prompt is on top of the +. not below it.
                    setTimeout(() => {
                        popperElement.style.marginTop = '-40px'; // Use marginTop instead of top
                    }, 0);
                }
            });
        }
    }

    show() {
        this.editor.setEditable(false);
        this.tippy?.show();
        this.componentStore.setOpen(true);
    }

    /**
     * Hide the tooltip but ignore store update if coming from ai-content-prompt.component.html keyup event.
     *
     * @param notifyStore
     */
    hide(notifyStore = true) {
        this.tippy?.hide();
        if (notifyStore) {
            this.componentStore.setOpen(false);
        }

        this.editor.setEditable(true);

        this.editor.view.focus();
    }

    destroy() {
        this.tippy?.destroy();
        this.destroy$.next(true);
        this.destroy$.complete();
        this.editor.view.dom.removeEventListener('click', this.handleClick.bind(this));
    }

    handleClick(_event: MouseEvent): void {
        if (this.storeSate.open && !this.storeSate.loading) {
            this.hide();
        }
    }
}

export const aiContentPromptPlugin = (options: AIContentPromptProps) => {
    return new Plugin({
        key: options.pluginKey as PluginKey,
        view: (view) => new AIContentPromptView({ view, ...options }),
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
                const { open } = transaction.getMeta(AI_CONTENT_PROMPT_PLUGIN_KEY) || {};
                const state = AI_CONTENT_PROMPT_PLUGIN_KEY.getState(oldState);

                if (typeof open === 'boolean') {
                    return { open };
                }

                // keep the old state in case we do not receive a new one.
                return state || value;
            }
        }
    });
};
