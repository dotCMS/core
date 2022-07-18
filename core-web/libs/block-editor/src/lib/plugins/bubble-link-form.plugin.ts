import { Editor, posToDOMRect } from '@tiptap/core';
import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import tippy, { Instance, Props } from 'tippy.js';

import { ComponentRef } from '@angular/core';
import {
    BubbleMenuLinkFormComponent,
    blockLinkMenuForm
} from '../extensions/components/bubble-menu-link-form/bubble-menu-link-form.component';

// Interface
import { PluginStorage } from '../extensions/bubble-link-form.extension';
import { isValidURL } from '../utils/bubble-menu.utils';

interface PluginState {
    toggle: boolean;
}

export interface BubbleLinkFormProps {
    pluginKey: PluginKey;
    editor: Editor;
    element: HTMLElement;
    tippyOptions?: Partial<Props>;
    storage: PluginStorage;
    component?: ComponentRef<BubbleMenuLinkFormComponent>;
}

export type BubbleLinkFormViewProps = BubbleLinkFormProps & {
    view: EditorView;
};

export class BubbleLinkFormView {
    public editor: Editor;

    public element: HTMLElement;

    public view: EditorView;

    public tippy: Instance | undefined;

    public tippyOptions?: Partial<Props>;

    public pluginKey: PluginKey;

    public component?: ComponentRef<BubbleMenuLinkFormComponent>;

    public storage: PluginStorage;

    private scrollElementMap = {
        'editor-suggestion-list': true,
        'editor-input-link': true,
        'editor-input-link-dropdown': true,
    };

    constructor({
        editor,
        element,
        view,
        tippyOptions = {},
        pluginKey,
        storage,
        component
    }: BubbleLinkFormViewProps) {
        this.editor = editor;
        this.element = element;
        this.view = view;

        this.tippyOptions = tippyOptions;

        // Detaches menu content from its current parent
        this.element.remove();
        this.element.style.visibility = 'visible';
        this.pluginKey = pluginKey;
        this.component = component;
        this.storage = storage;

        this.editor.on('focus', this.focusHandler);
        this.setComponentEvents();

        // We need to also react to page scrolling.
        document.body.addEventListener('scroll', this.hanlderScroll.bind(this), true);
    }

    update(view: EditorView, prevState?: EditorState): void {
        const next = this.pluginKey.getState(view.state);
        const prev = this.pluginKey.getState(prevState);

        // Check that the current plugin state is different to previous plugin state.
        if (next.toggle === prev.toggle) {
            this.detectLinkFormChanges();
            return;
        }

        this.createTooltip();

        this.tippy?.state.isVisible ? this.hide() : this.show();
        this.detectLinkFormChanges();
    }

    focusHandler = () => {
        if (this.tippy?.state.isVisible) {
            this.hide();
        }
    };

    createTooltip() {
        const { element: editorElement } = this.editor.options;
        const editorIsAttached = !!editorElement.parentElement;

        if (this.tippy || !editorIsAttached) {
            return;
        }

        this.tippy = tippy(editorElement, {
            appendTo: document.body,
            duration: 250,
            getReferenceClientRect: null,
            content: this.element,
            interactive: true,
            trigger: 'manual',
            placement: 'bottom-start',
            hideOnClick: 'toggle',
            ...this.tippyOptions
        });
    }

    show() {
        this.tippy?.show();
        this.component.instance.showSuggestions = false;
        // Afther show the component set values
        this.setInputValues();
        this.component.instance.items = [];
        this.component.instance.focusInput();
        this.tippy?.setProps({ getReferenceClientRect: () => this.setTippyPosition() });
    }

    hide() {
        this.tippy?.hide();
        // After show the component focus editor
        this.editor.view.focus();
        this.editor.commands.unsetHighlight();
    }

    setTippyPosition(): DOMRect {
        // Get Node Position
        const { view } = this.editor;
        const { state } = view;
        const { doc, selection } = state;
        const { ranges } = selection;
        const from = Math.min(...ranges.map((range) => range.$from.pos));
        const to = Math.max(...ranges.map((range) => range.$to.pos));
        const nodeClientRect = posToDOMRect(view, from, to);

        // Get Editor Container Position
        const { element: editorElement } = this.editor.options;
        const editorClientRect = editorElement.parentElement.getBoundingClientRect();
        const bubbleMenuRect = document.querySelector('#bubble-menu').getBoundingClientRect();

        // Check for an overflow in the content
        const isOverflow = editorClientRect.bottom < nodeClientRect.bottom;

        // Check if the node is a dotImage
        const node = doc?.nodeAt(from);
        const isNodeImage = node.type.name === 'dotImage';

        // If there is an overflow, use bubble menu position as a reference.
        return isOverflow || isNodeImage ? bubbleMenuRect : nodeClientRect;
    }

    addLink({ link, blank = false }) {
        if (this.isDotImageNode()) {
            this.editor.commands.setImageLink({ href: link });
        } else {
            this.editor.commands.setLink({ href: link, target: blank ? '_blank' : '_top' });
        }
        this.hide();
    }

    removeLink() {
        if (this.isDotImageNode()) {
            this.editor.commands.unsetImageLink();
        } else {
            this.editor.commands.unsetLink();
        }
        this.hide();
    }

    setInputValues() {
        const values = this.getLinkProps();
        this.component.instance.initialValues = values;
        this.component.instance.setFormValue(values);
    }

    setComponentEvents() {
        this.component.instance.hide.subscribe(() => this.hide());
        this.component.instance.removeLink.subscribe(() => this.removeLink());
        this.component.instance.submitForm.subscribe((event) => this.addLink(event));
    }

    detectLinkFormChanges() {
        this.component.changeDetectorRef.detectChanges();
    }

    getLinkProps(): blockLinkMenuForm {
        const { href = '', target } = this.editor.isActive('link')
            ? this.editor.getAttributes('link')
            : this.editor.getAttributes('dotImage');
        const link = href || this.getLinkSelect();
        const blank = target ? target === '_blank' : true;
        return { link, blank };
    }

    getLinkSelect() {
        const { state } = this.editor;
        const { from, to } = state.selection;
        const text = state.doc.textBetween(from, to, ' ');

        return isValidURL(text) ? text : '';
    }

    isDotImageNode() {
        const { type } = this.editor.state.doc.nodeAt(this.editor.state.selection.from);
        return type.name === 'dotImage';
    }

    destroy() {
        this.tippy?.destroy();
        this.editor.off('focus', this.focusHandler);
    }

    private hanlderScroll(e: Event) {
        const element = e.target as HTMLElement;
        const parentElement = element?.parentElement?.parentElement;
        // When the text is too long, the input fires the `scroll` event.
        // When that happens, we do not want to hide the tippy.
        if (this.scrollElementMap[element.id] || this.scrollElementMap[parentElement.id]) {
            return;
        }
        this.tippy?.hide();
    }
}

export const bubbleLinkFormPlugin = (options: BubbleLinkFormProps) => {
    return new Plugin({
        key: options.pluginKey as PluginKey,
        view: (view) => new BubbleLinkFormView({ view, ...options }),
        state: {
            init(): PluginState {
                return {
                    toggle: true
                };
            },

            apply(transaction: Transaction): PluginState {
                const transactionMeta = transaction.getMeta(options.pluginKey);
                if (transactionMeta) {
                    return {
                        toggle: options.storage.show
                    };
                }

                return {
                    toggle: options.storage.show
                };
            }
        }
    });
};
