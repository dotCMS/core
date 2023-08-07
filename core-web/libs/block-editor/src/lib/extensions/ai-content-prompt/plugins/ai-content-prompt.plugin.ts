import { Node } from 'prosemirror-model';
import { EditorState, NodeSelection, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Subject } from 'rxjs';
import tippy, { Instance, Props } from 'tippy.js';

import { ComponentRef } from '@angular/core';

import { takeUntil } from 'rxjs/operators';

import { Editor, posToDOMRect } from '@tiptap/core';
import { BubbleMenuView } from '@tiptap/extension-bubble-menu';

import { getNodePosition } from '../../bubble-menu/utils';
import { AIContentPromptComponent } from '../ai-content-prompt.component';
import { AI_CONTENT_PROMPT_PLUGIN_KEY } from '../ai-content-prompt.extension';
import { AI_PROMPT_DYNAMIC_CONTROLS, TIPPY_OPTIONS } from '../utils';

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

    public node: Node;

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

        this.component.instance.buildForm(AI_PROMPT_DYNAMIC_CONTROLS);

        this.component.instance.formValues.pipe(takeUntil(this.$destroy)).subscribe((data) => {
            this.editor.commands.updateValue(data);
        });

        this.element.addEventListener('mousedown', this.mousedownHandler, { capture: true });

        this.component.instance.hide.pipe(takeUntil(this.$destroy)).subscribe(() => {
            this.editor.commands.closeAIPrompt();
        });

        this.editor.on('focus', this.focusHandler);

        document.body.addEventListener('scroll', this.hanlderScroll.bind(this), true);
    }

    update(view: EditorView, prevState?: EditorState) {
        const next = this.pluginKey?.getState(view.state);
        const prev = prevState ? this.pluginKey?.getState(prevState) : { open: false };

        const { state } = view;
        const { doc, selection } = state;

        const { ranges } = selection;
        const from = Math.min(...ranges.map((range) => range.$from.pos));
        const to = Math.max(...ranges.map((range) => range.$to.pos));

        if (next?.open === prev?.open) {
            this.tippy?.popperInstance?.forceUpdate();

            return;
        }

        if (next.open) {
            this.component.instance.buildForm(next.form || AI_PROMPT_DYNAMIC_CONTROLS);
        } else {
            this.component.instance.cleanForm();
        }

        this.createTooltip();

        this.tippy?.setProps({
            getReferenceClientRect: () => {
                if (selection instanceof NodeSelection) {
                    const node = view.nodeDOM(from) as HTMLElement;

                    if (node) {
                        this.node = doc.nodeAt(from);
                        const type = this.node.type.name;

                        return this.tippyRect(node, type);
                    }
                }

                return posToDOMRect(view, from, to);
            }
        });

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
            onShow: (instance) => {
                (instance.popper as HTMLElement).style.width = '100%';

                requestAnimationFrame(() => {
                    this.component.instance.input.nativeElement.focus();
                });
            }
        });
    }

    focusHandler = () => {
        const { state } = this.editor;
        const { open } = AI_CONTENT_PROMPT_PLUGIN_KEY.getState(state);
        const pluginState = this.pluginKey.getState(state);

        if (!pluginState.open) {
            return;
        }

        if (open) {
            this.editor.commands.closeForm();
            requestAnimationFrame(() => {
                this.component.instance.input.nativeElement.focus();
            });
        } else {
            this.editor.commands.closeForm();
        }

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
        takeUntil(this.$destroy);
        this.$destroy.complete();
        this.editor.off('focus', this.focusHandler);
    }

    private tippyRect(node, type) {
        const domRect = document.querySelector('#ai-text-prompt')?.getBoundingClientRect();

        return domRect || getNodePosition(node, type);
    }

    private hanlderScroll(e: Event) {
        if (this.shouldHideOnScroll(e.target as HTMLElement)) {
            return true;
        }

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
