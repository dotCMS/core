import { ComponentRef } from '@angular/core';
import { Subject } from 'rxjs';

import { Editor, isNodeSelection, posToDOMRect } from '@tiptap/core';
import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Instance, Props } from 'tippy.js';
import { BubbleMenuView } from '@tiptap/extension-bubble-menu';
import { BUBBLE_FORM_PLUGIN_KEY } from '../bubble-form.extension';
import tippy from 'tippy.js';
import { BubbleFormComponent } from '../bubble-form.component';

export interface BubbleFormProps {
    pluginKey: PluginKey;
    editor: Editor;
    element: HTMLElement;
    tippyOptions?: Partial<Props>;
    component?: ComponentRef<BubbleFormComponent>;
}

export type BubbleFormViewProps = BubbleFormProps & {
    view: EditorView;
};

interface PluginState {
    open: boolean;
}

export class BubbleFormView extends BubbleMenuView {
    public editor: Editor;

    public element: HTMLElement;

    public view: EditorView;

    public tippy: Instance | undefined;

    public tippyOptions?: Partial<Props>;

    public pluginKey: PluginKey;

    public component?: ComponentRef<BubbleFormComponent>;

    private $destroy = new Subject<boolean>();

    constructor(props: BubbleFormViewProps) {
        const { editor, element, view, tippyOptions = {}, pluginKey, component } = props;

        super(props);
        this.editor = editor;
        this.element = element;
        this.view = view;

        this.tippyOptions = tippyOptions;

        // Detaches menu content from its current parent
        this.element.remove();
        this.element.style.visibility = 'visible';
        this.pluginKey = pluginKey;
        this.component = component;

        this.component.instance.formValues.subscribe((data: Record<string, unknown>) => {
            this.editor.commands.setImage({
                ...data,
                src: 'https://media.istockphoto.com/vectors/costa-rica-vector-id652225694?s=170667a'
            });
        });

        this.editor.on('focus', this.focusHandler);
    }

    update(view: EditorView, prevState?: EditorState): void {
        const next = this.pluginKey?.getState(view.state);
        const prev = prevState ? this.pluginKey?.getState(prevState) : { open: false };

        const { state } = view;
        const { selection } = state;

        const { ranges } = selection;
        const from = Math.min(...ranges.map((range) => range.$from.pos));
        const to = Math.max(...ranges.map((range) => range.$to.pos));

        // Check that the current plugin state is different to previous plugin state.
        if (next?.open === prev?.open) {
            return;
        }

        this.createTooltip();

        this.tippy?.setProps({
            getReferenceClientRect:
                this.tippyOptions?.getReferenceClientRect ||
                (() => {
                    if (isNodeSelection(state.selection)) {
                        const node = view.nodeDOM(from) as HTMLElement;

                        if (node) {
                            return node.getBoundingClientRect();
                        }
                    }

                    return posToDOMRect(view, from, to);
                })
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
            duration: 250,
            content: this.element,
            interactive: true,
            maxWidth: 'none',
            trigger: 'manual',
            placement: 'bottom-start',
            hideOnClick: 'toggle',
            popperOptions: {
                modifiers: [
                    {
                        name: 'flip',
                        options: { fallbackPlacements: ['top-start'] }
                    }
                ]
            }
        });
    }

    focusHandler = () => {
        this.editor.commands.closeForm();
        // we use `setTimeout` to make sure `selection` is already updated
        setTimeout(() => this.update(this.editor.view));
    };

    show() {
        const { alt, src, title } = this.editor.getAttributes('image');
        this.component.instance.setFormData({ alt, src, title });

        this.tippy?.show();
    }

    destroy() {
        this.tippy?.destroy();
        // this.editor.off('focus', this.focusHandler);
        this.$destroy.next(true);
        this.component.destroy();
    }
}

export const bubbleFormPlugin = (options: BubbleFormViewProps) => {
    return new Plugin({
        key: options.pluginKey as PluginKey,
        view: (view) => new BubbleFormView({ view, ...options }),
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
                const { open } = transaction.getMeta(BUBBLE_FORM_PLUGIN_KEY) || {};
                const state = BUBBLE_FORM_PLUGIN_KEY?.getState(oldState);

                if (typeof open === 'boolean') {
                    return { open };
                }

                // keep the old state in case we do not receive a new one.
                return state || value;
            }
        }
    });
};
