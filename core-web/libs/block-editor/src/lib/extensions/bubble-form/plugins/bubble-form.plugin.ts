import { ComponentRef } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { EditorState, Plugin, PluginKey, Transaction, NodeSelection } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Editor, posToDOMRect } from '@tiptap/core';
import tippy, { Instance, Props } from 'tippy.js';

import { BubbleMenuView } from '@tiptap/extension-bubble-menu';
import { BUBBLE_FORM_PLUGIN_KEY } from '../bubble-form.extension';
import { BubbleFormComponent, DynamicControl } from '../bubble-form.component';

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

const controls: DynamicControl<string>[] = [
    {
        key: 'src',
        label: 'path',
        required: true,
        controlType: 'text',
        type: 'text'
    },
    {
        key: 'alt',
        label: 'alt',
        required: true,
        controlType: 'text',
        type: 'text'
    },
    {
        key: 'title',
        label: 'caption',
        required: true,
        controlType: 'text',
        type: 'text'
    }
];

interface PluginState {
    open: boolean;
}

// Move this an util file.
export const getNodePosition = (node: HTMLElement, type: string): DOMRect => {
    // If is a image Node, get the image position
    if (type === 'image') {
        const rect = node.getElementsByTagName('img')[0].getBoundingClientRect().toJSON();

        const newRect = {
            ...rect,
            height: 20
        };

        return newRect as DOMRect;
    }

    return node.getBoundingClientRect();
};

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

        this.component.instance.dynamicControls = controls;
        this.component.instance.buildForm();

        this.component.instance.formValues.pipe(takeUntil(this.$destroy)).subscribe((data) => {
            this.editor.commands.setImage({ ...data });
            this.editor.commands.closeForm();
        });

        this.element.addEventListener('mousedown', this.mousedownHandler, { capture: true });
        this.editor.on('focus', this.focusHandler);
    }

    update(view: EditorView, prevState?: EditorState): void {
        const next = this.pluginKey?.getState(view.state);
        const prev = prevState ? this.pluginKey?.getState(prevState) : { open: false };

        const { state } = view;
        const { doc, selection } = state;

        const { ranges } = selection;
        const from = Math.min(...ranges.map((range) => range.$from.pos));
        const to = Math.max(...ranges.map((range) => range.$to.pos));

        // Check that the current plugin state is different to previous plugin state.
        if (next?.open === prev?.open) {
            return;
        }

        this.createTooltip();

        this.tippy?.setProps({
            getReferenceClientRect: () => {
                if (selection instanceof NodeSelection) {
                    const node = view.nodeDOM(from) as HTMLElement;

                    if (node) {
                        const type = doc.nodeAt(from).type.name;

                        return getNodePosition(node, type);
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
            duration: 250,
            content: this.element,
            interactive: true,
            maxWidth: 'none',
            trigger: 'manual',
            placement: 'bottom',
            hideOnClick: 'toggle',
            popperOptions: {
                modifiers: [
                    {
                        name: 'flip',
                        options: { fallbackPlacements: ['top'] }
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
        this.component.instance.setFormValues({ alt, src, title });

        this.tippy?.show();
    }

    destroy() {
        this.tippy?.destroy();
        // this.editor.off('focus', this.focusHandler);
        this.$destroy.next(true);
        this.component.destroy();
        this.component.instance.formValues.unsubscribe();
    }
}

export const bubbleFormPlugin = (options: BubbleFormProps) => {
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
