import { ComponentRef } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { EditorState, Plugin, PluginKey, Transaction, NodeSelection } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Node } from 'prosemirror-model';

import { Editor, posToDOMRect } from '@tiptap/core';
import { BubbleMenuView } from '@tiptap/extension-bubble-menu';
import tippy, { Instance, Props } from 'tippy.js';
import { imageFormControls } from '../utils';

import { getNodePosition, BUBBLE_FORM_PLUGIN_KEY, BubbleFormComponent } from '@dotcms/block-editor';

export interface BubbleFormProps {
    pluginKey: PluginKey;
    editor: Editor;
    element: HTMLElement;
    tippyOptions?: Partial<Props>;
    component?: ComponentRef<BubbleFormComponent>;
    form$: Observable<{ [key: string]: string }>;
}

export type BubbleFormViewProps = BubbleFormProps & {
    view: EditorView;
};

interface PluginState {
    open: boolean;
    form: [];
    options: { customClass: string };
}

export class BubbleFormView extends BubbleMenuView {
    public editor: Editor;

    public node: Node;

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

        this.component.instance.buildForm(imageFormControls);

        this.component.instance.formValues.pipe(takeUntil(this.$destroy)).subscribe((data) => {
            this.editor.commands.updateValue(data);
        });

        this.component.instance.hide.pipe(takeUntil(this.$destroy)).subscribe(() => {
            this.editor.commands.closeForm();
        });
        this.element.addEventListener('mousedown', this.mousedownHandler, { capture: true });
        this.editor.on('focus', this.focusHandler);

        // We need to also react to page scrolling.
        document.body.addEventListener('scroll', this.hanlderScroll.bind(this), true);
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
            this.tippy?.popperInstance?.forceUpdate();

            return;
        }

        if (next.open && next.form) {
            this.component.instance.buildForm(next.form);
            this.component.instance.options = next.options;
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
            },

            onShow: () => {
                requestAnimationFrame(() => {
                    this.component.instance.inputs.first.nativeElement.focus();
                });
            }
        });
    }

    focusHandler = () => {
        this.editor.commands.closeForm();
        // we use `setTimeout` to make sure `selection` is already updated
        setTimeout(() => this.update(this.editor.view));
    };

    show() {
        this.tippy?.show();
    }

    destroy() {
        this.tippy?.destroy();
        this.editor.off('focus', this.focusHandler);
        this.$destroy.next(true);
        this.component.destroy();
        this.component.instance.formValues.unsubscribe();
    }

    private hanlderScroll(e: Event) {
        if (this.tippy?.popper && this.tippy?.popper.contains(e.target as HTMLElement)) {
            return true;
        }

        this.editor.commands.closeForm();

        // we use `setTimeout` to make sure `selection` is already updated
        setTimeout(() => this.update(this.editor.view));
    }

    private tippyRect(node, type) {
        const domRect = document.querySelector('#bubble-menu')?.getBoundingClientRect();

        return domRect || getNodePosition(node, type);
    }
}

export const bubbleFormPlugin = (options: BubbleFormProps) => {
    return new Plugin({
        key: options.pluginKey as PluginKey,
        view: (view) => new BubbleFormView({ view, ...options }),
        state: {
            init(): PluginState {
                return {
                    open: false,
                    form: [],
                    options: null
                };
            },

            apply(
                transaction: Transaction,
                value: PluginState,
                oldState: EditorState
            ): PluginState {
                const { open, form, options } = transaction.getMeta(BUBBLE_FORM_PLUGIN_KEY) || {};
                const state = BUBBLE_FORM_PLUGIN_KEY?.getState(oldState);

                if (typeof open === 'boolean') {
                    return { open, form, options };
                }

                // keep the old state in case we do not receive a new one.
                return state || value;
            }
        }
    });
};
