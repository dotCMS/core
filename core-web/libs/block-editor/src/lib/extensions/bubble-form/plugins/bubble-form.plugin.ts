import { Node } from 'prosemirror-model';
import { EditorState, NodeSelection, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Observable, Subject, Subscription } from 'rxjs';
import tippy, { Instance, Props } from 'tippy.js';

import { ComponentRef } from '@angular/core';

import { takeUntil } from 'rxjs/operators';

import { Editor, posToDOMRect } from '@tiptap/core';
import { BubbleMenuView } from '@tiptap/extension-bubble-menu';

import { BASIC_TIPPY_OPTIONS, getNodePosition } from '../../../shared';
import { BubbleFormComponent } from '../bubble-form.component';
import { BUBBLE_FORM_PLUGIN_KEY } from '../bubble-form.extension';
import { imageFormControls } from '../utils';

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
    public override editor: Editor;

    public node: Node;

    public override element: HTMLElement;

    public override view: EditorView;

    public override tippy: Instance | undefined;

    public override tippyOptions?: Partial<Props>;

    public pluginKey: PluginKey;

    public component?: ComponentRef<BubbleFormComponent>;

    private $destroy = new Subject<boolean>();

    private formValuesSubscription: Subscription;

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

        this.formValuesSubscription = this.component.instance.formValues
            .pipe(takeUntil(this.$destroy))
            .subscribe((data) => {
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

    override update(view: EditorView, prevState?: EditorState): void {
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

    override createTooltip() {
        const { element: editorElement } = this.editor.options;
        const editorIsAttached = !!editorElement.parentElement;

        if (this.tippy || !editorIsAttached) {
            return;
        }

        this.tippy = tippy(editorElement.parentElement, {
            ...this.tippyOptions,
            ...BASIC_TIPPY_OPTIONS,
            content: this.element,
            onShow: () => {
                requestAnimationFrame(() => {
                    // firefox validation because of https://github.com/dotCMS/core/issues/30327
                    const isFirefox = /firefox/i.test(navigator.userAgent);
                    if (!isFirefox) {
                        this.component.instance.inputs.first.nativeElement.focus();
                    }
                });
            }
        });
    }

    override focusHandler = () => {
        this.editor.commands.closeForm();
        // we use `setTimeout` to make sure `selection` is already updated
        setTimeout(() => this.update(this.editor.view));
    };

    override show() {
        this.tippy?.show();
    }

    override destroy() {
        this.tippy?.destroy();
    }

    private hanlderScroll(e: Event) {
        if (!this.shouldHideOnScroll(e.target as HTMLElement)) {
            return true;
        }

        // we use `setTimeout` to make sure `selection` is already updated
        setTimeout(() => this.update(this.editor.view));

        return null;
    }

    private tippyRect(node, type) {
        const domRect = document.querySelector('#bubble-menu')?.getBoundingClientRect();

        return domRect || getNodePosition(node, type);
    }

    private shouldHideOnScroll(node: HTMLElement): boolean {
        return this.tippy?.state.isMounted && this.tippy?.popper.contains(node);
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
