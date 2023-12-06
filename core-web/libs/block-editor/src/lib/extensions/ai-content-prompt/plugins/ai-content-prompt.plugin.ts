import { Node } from 'prosemirror-model';
import { EditorState, Plugin, PluginKey, TextSelection, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Subject } from 'rxjs';
import tippy, { Instance, Props } from 'tippy.js';

import { ComponentRef } from '@angular/core';

import { filter, skip, takeUntil, tap } from 'rxjs/operators';

import { Editor } from '@tiptap/core';

import { DotTiptapNodeInformation, findNodeByType, replaceNodeWithContent } from '../../../shared';
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

    public tippy: Instance | undefined;

    public tippyOptions: Partial<Props>;

    public pluginKey: PluginKey;

    public component: ComponentRef<AIContentPromptComponent>;

    private componentStore: AiContentPromptStore;

    private storeSate: AiContentPromptState;

    private destroy$ = new Subject<boolean>();

    private boundClickHandler = this.handleClick.bind(this);

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
                this.editor
                    .chain()
                    .closeAIPrompt()
                    .insertAINode(content)
                    .openAIContentActions(DOT_AI_TEXT_CONTENT_KEY)
                    .run();
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
                const nodeInformation: DotTiptapNodeInformation = findNodeByType(
                    this.editor,
                    NodeTypes.AI_CONTENT
                );
                replaceNodeWithContent(this.editor, nodeInformation, state.content);

                this.componentStore.setAcceptContent(false);
            });

        /**
         * Subscription to close the tippy since that can happen on escape listener that is in the html
         * template in ai-content-prompt.component.html
         */
        this.componentStore.exit$
            .pipe(
                skip(1),
                takeUntil(this.destroy$),
                filter((exit) => exit)
            )
            .subscribe(() => {
                this.tippy?.hide();
                this.componentStore.setExit(false);
            });

        /**
         * Subscription to delete AI_CONTENT node.
         * Fired from the AI Content Actions plugin.
         */
        this.componentStore.deleteContent$
            .pipe(
                skip(1),
                takeUntil(this.destroy$),
                filter((deleteContent) => deleteContent)
            )
            .subscribe(() => {
                const nodeInformation: DotTiptapNodeInformation = findNodeByType(
                    this.editor,
                    NodeTypes.AI_CONTENT
                );

                if (nodeInformation) {
                    this.editor.commands.deleteRange({
                        from: nodeInformation.from,
                        to: nodeInformation.to
                    });
                }

                this.componentStore.setDeleteContent(false);
            });
    }

    update(view: EditorView, prevState?: EditorState) {
        const next = this.pluginKey?.getState(view.state);
        const prev = prevState ? this.pluginKey?.getState(prevState) : { open: false };

        if (next?.open === prev?.open) {
            this.tippy?.popperInstance?.forceUpdate();

            return;
        }

        next.open ? this.show() : this.hide(this.storeSate.open);
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
        this.createTooltip();
        this.manageClickListener(true);
        this.editor.setEditable(false);
        this.tippy?.show();
        this.componentStore.setOpen(true);
    }

    /**
     * Hide the tooltip but ignore store update  if open is false already
     * this happens when the event comes from ai-content-prompt.component.html escape keyup event.
     *
     * @param notifyStore
     */
    hide(notifyStore = true) {
        this.editor.setEditable(true);

        this.editor.view.focus();
        if (notifyStore) {
            this.componentStore.setOpen(false);
        }

        this.manageClickListener(false);
    }

    destroy() {
        this.tippy?.destroy();
        this.destroy$.next(true);
        this.destroy$.complete();
        this.manageClickListener(false);
    }

    /**
     * Handles the click event on the editor's DOM. If the AI content prompt is open
     * and not in a loading state, this function hides the associated Tippy tooltip.
     */
    handleClick(): void {
        if (this.storeSate.open && !this.storeSate.loading) {
            this.tippy.hide();
        }
    }

    /**
     * Manages the click event listener on the editor's DOM based on the specified condition.
     * If `addListener` is `true`, the click event listener is added; otherwise, it is removed.
     *
     * @param addListener - A boolean indicating whether to add or remove the click event listener.
     */
    manageClickListener(addListener: boolean): void {
        addListener
            ? this.editor.view.dom.addEventListener('click', this.boundClickHandler)
            : this.editor.view.dom.removeEventListener('click', this.boundClickHandler);
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
