import { Editor, posToDOMRect, isNodeSelection } from '@tiptap/core';
import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import tippy, { Instance, Props } from 'tippy.js';

import { ComponentRef } from '@angular/core';
import { BubbleMenuLinkFormComponent } from '../extensions/components/bubble-menu-link-form/bubble-menu-link-form.component';

// Interface
import { PluginStorage } from '../extensions/bubble-link-form.extension';

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
        this.createTooltip();
    }

    update(view: EditorView, prevState?: EditorState): void {
        const prePluginState = this.pluginKey.getState(view.state);
        const currentPluginState = this.pluginKey.getState(prevState);

        // Check that the current plugin state is different to previous plugin state.
        if (prePluginState.toggle === currentPluginState.toggle) {
            this.detectLinkFormChanges();
            return;
        }
        
        this.tippy?.state.isVisible ? this.hide() : this.show();
        this.detectLinkFormChanges();
    }

    focusHandler = () => {
        if (this.tippy?.state.isVisible) {
            this.hide();
        }
    };

    createTooltip() {
        if (this.tippy) {
            return;
        }

        this.tippy = tippy(this.editor.view.dom, {
            duration: 250,
            getReferenceClientRect: null,
            content: this.element,
            interactive: true,
            trigger: 'manual',
            placement: 'bottom',
            hideOnClick: 'toggle',
            ...this.tippyOptions
        });
    }

    show() {
        this.tippy?.show();
        // Afther show the component set values
        this.setInputValues();
        this.focusInput();
        this.setTippyPosition();
    }

    hide() {
        this.tippy?.hide();
        // Afther show the component focus editor
        this.editor.view.focus();
        this.editor.commands.unsetHighlight();
    }

    setTippyPosition() {
        const { view } = this.editor;
        const { state } = view;
        const { selection } = state;
        const { ranges } = selection;
        const from = Math.min(...ranges.map((range) => range.$from.pos));
        const to = Math.max(...ranges.map((range) => range.$to.pos));
        this.tippy.setProps({
            getReferenceClientRect: () => {
                if (isNodeSelection(state.selection)) {
                    const node = view.nodeDOM(from) as HTMLElement;
                    if (node) {
                        return node.getBoundingClientRect();
                    }
                }
                return posToDOMRect(view, from, to);
            }
        });
    }

    addLink(link: string) {
        if (link) {
            this.editor.commands.setLink({ href: link });
        }
        this.hide();
    }

    removeLink() {
        this.editor.commands.unsetLink();
        this.hide();
    }

    setInputValues() {
        this.component.instance.nodeLink = this.getNodeLink();
        this.component.instance.newLink = this.getNodeLink() || this.getLinkSelect();
    }

    focusInput() {
        this.component.instance.focusInput();
    }

    setComponentEvents() {
        this.component.instance.hideForm.subscribe(() => this.hide());
        this.component.instance.removeLink.subscribe(() => this.removeLink());
        this.component.instance.setLink.subscribe((event) => this.addLink(event));
    }

    detectLinkFormChanges() {
        this.component.changeDetectorRef.detectChanges();
    }

    getNodeLink(): string {
        return this.editor.isActive('link') ? this.editor.getAttributes('link').href : '';
    }

    getLinkSelect() {
        const { state } = this.editor;
        const { from, to } = state.selection;
        const text = state.doc.textBetween(from, to, ' ');

        return this.isLink(text) ? text : '';
    }

    isLink(nodeText: string) {
        const pattern = new RegExp(
            '^(https?:\\/\\/)?' + // protocol
                '((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|' + // domain name
                '((\\d{1,3}\\.){3}\\d{1,3}))' + // OR ip (v4) address
                '(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*' + // port and path
                '(\\?[;&a-z\\d%_.~+=-]*)?' + // query string
                '(\\#[-a-z\\d_]*)?$',
            'i'
        ); // fragment locator
        return !!pattern.test(nodeText);
    }

    destroy() {
        this.tippy?.destroy();
        this.editor.off('focus', this.focusHandler);
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
