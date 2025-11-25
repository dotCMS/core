import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Subject } from 'rxjs';
import tippy, { Instance, Props } from 'tippy.js';

import { ComponentRef } from '@angular/core';

import { takeUntil } from 'rxjs/operators';

import { Editor, posToDOMRect } from '@tiptap/core';

import { isEqual } from '@dotcms/utils';

import { ImageNode } from '../../../nodes';
import { BASIC_TIPPY_OPTIONS, getPosAtDocCoords } from '../../../shared';
import { isValidURL } from '../../bubble-menu/utils';
import { BubbleLinkFormComponent, NodeProps } from '../bubble-link-form.component';
import { LINK_FORM_PLUGIN_KEY } from '../bubble-link-form.extension';
import { openFormLinkOnclik } from '../utils';

interface PluginState {
    isOpen: boolean;
    openOnClick: boolean;
}

export interface BubbleLinkFormProps {
    pluginKey: PluginKey;
    editor: Editor;
    element: HTMLElement;
    tippyOptions?: Partial<Props>;
    component?: ComponentRef<BubbleLinkFormComponent>;
    languageId: number;
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

    public component?: ComponentRef<BubbleLinkFormComponent>;

    public languageId: number;

    private scrollElementMap = {
        'editor-suggestion-list': true,
        'editor-input-link': true,
        'editor-input-checkbox': true
    };

    private $destroy = new Subject<boolean>();

    constructor({
        editor,
        element,
        view,
        tippyOptions = {},
        pluginKey,
        component,
        languageId
    }: BubbleLinkFormViewProps) {
        this.editor = editor;
        this.element = element;
        this.view = view;

        this.languageId = languageId;

        this.tippyOptions = tippyOptions;

        // Detaches menu content from its current parent
        this.element.remove();
        this.element.style.visibility = 'visible';
        this.pluginKey = pluginKey;
        this.component = component;

        this.editor.on('focus', this.focusHandler);
        this.setComponentEvents();

        // We need to also react to page scrolling.
        document.body.addEventListener('scroll', this.hanlderScroll.bind(this), true);
    }

    update(view: EditorView, prevState?: EditorState): void {
        const next = this.pluginKey.getState(view.state);
        const prev = this.pluginKey.getState(prevState);

        // Check that the current plugin state is different to previous plugin state.
        if (next.isOpen === prev.isOpen) {
            this.detectLinkFormChanges();

            return;
        }

        this.createTooltip();
        next.isOpen ? this.show() : this.hide();
        this.detectLinkFormChanges();
    }

    focusHandler = () => {
        const { state } = this.editor;
        const { to } = state.selection;
        const { openOnClick } = LINK_FORM_PLUGIN_KEY.getState(state);
        const pluginState = this.pluginKey.getState(state);

        if (!pluginState.isOpen) {
            return;
        }

        if (openOnClick) {
            this.editor.commands.closeLinkForm();
            requestAnimationFrame(() => this.editor.commands.setTextSelection(to));
        } else {
            this.editor.commands.closeLinkForm();
        }
    };

    createTooltip() {
        const { element: editorElement } = this.editor.options;
        const editorIsAttached = !!editorElement.parentElement;

        if (this.tippy || !editorIsAttached) {
            return;
        }

        this.tippy = tippy(editorElement.parentElement, {
            ...this.tippyOptions,
            ...BASIC_TIPPY_OPTIONS,
            getReferenceClientRect: () => this.setTippyPosition(),
            content: this.element,
            onHide: () => {
                this.editor.commands.closeLinkForm();
            }
        });
    }

    show() {
        this.tippy?.show();
        this.component.instance.showSuggestions = false;
        // Afther show the component set values
        this.setInputValues();
        this.component.instance.focusInput();
    }

    hide() {
        this.tippy?.hide();
        // After show the component focus editor
        this.editor.view.focus();
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
        const domRect =
            document.querySelector('#bubble-menu')?.getBoundingClientRect() || nodeClientRect;

        // Check for an overflow in the content
        const isOverflow = editorClientRect.bottom < nodeClientRect.bottom;

        // Check if the node is a dotImage
        const node = doc?.nodeAt(from);
        const isNodeImage = node?.type.name === ImageNode.name;

        // If there is an overflow, use bubble menu position as a reference.
        return isOverflow || isNodeImage ? domRect : nodeClientRect;
    }

    setLinkValues({ link, blank = false }) {
        if (!link.length) {
            return;
        }

        const href = this.formatLink(link);
        const linkValue = { href, target: blank ? '_blank' : '_top' };

        this.isImageNode()
            ? this.editor.commands.setImageLink(linkValue)
            : this.editor.commands.setLink(linkValue);
    }

    removeLink() {
        this.isImageNode()
            ? this.editor.commands.unsetImageLink()
            : this.editor.commands.unsetLink();
        this.hide();
    }

    formatLink(link: string) {
        if (!isValidURL(link)) {
            return link;
        }

        const pattern = new RegExp('^(http|https)', 'i');
        const startWithProtocol = pattern.test(link);

        return startWithProtocol ? link : `http://${link}`;
    }

    setInputValues() {
        const values = this.getLinkProps();
        this.component.instance.initialValues = values;
        this.component.instance.languageId = this.languageId;
        this.component.instance.setFormValue(values.link ? values : { link: this.getLinkSelect() });
    }

    setComponentEvents() {
        this.component.instance.hide.pipe(takeUntil(this.$destroy)).subscribe(() => this.hide());
        this.component.instance.removeLink
            .pipe(takeUntil(this.$destroy))
            .subscribe(() => this.removeLink());
        // Update tippy manually when the suggestion is open.
        // Therefore, the tippy is not going to be cut when it does not have enough space.
        this.component.instance.isSuggestionOpen
            .pipe(takeUntil(this.$destroy))
            .subscribe(() => this.tippy.popperInstance.update());
        this.component.instance.setNodeProps
            .pipe(takeUntil(this.$destroy))
            .subscribe((event) => this.setLinkValues(event));
    }

    detectLinkFormChanges() {
        this.component.changeDetectorRef.detectChanges();
        requestAnimationFrame(() => this.tippy?.popperInstance?.forceUpdate());
    }

    getLinkProps(): NodeProps {
        const { href: link = '', target = '_top' } = this.editor.isActive('link')
            ? this.editor.getAttributes('link')
            : this.editor.getAttributes(ImageNode.name);
        const blank = target === '_blank';

        return { link, blank };
    }

    getLinkSelect() {
        const { state } = this.editor;
        const { from, to } = state.selection;
        const text = state.doc.textBetween(from, to, ' ');

        return isValidURL(text) ? text : '';
    }

    isImageNode() {
        const { type } = this.editor.state.doc.nodeAt(this.editor.state.selection.from) || {};

        return type?.name === ImageNode.name;
    }

    destroy() {
        this.tippy?.destroy();
        this.editor.off('focus', this.focusHandler);
        this.$destroy.next(true);
        this.component.destroy();
    }

    private hanlderScroll(e: Event) {
        if (!this.tippy?.state.isMounted) {
            return;
        }

        const element = e.target as HTMLElement;
        const parentElement = element?.parentElement?.parentElement;
        // If text is too long, the input fires the `scroll` event.
        // When that happens, we do not want to hide the tippy.
        if (this.scrollElementMap[element.id] || this.scrollElementMap[parentElement.id]) {
            return;
        }

        this.hide();
    }
}

export const bubbleLinkFormPlugin = (options: BubbleLinkFormProps) => {
    let lastNode;

    return new Plugin({
        key: options.pluginKey as PluginKey,
        view: (view) => new BubbleLinkFormView({ view, ...options }),
        state: {
            init(): PluginState {
                return {
                    isOpen: false,
                    openOnClick: false
                };
            },

            apply(
                transaction: Transaction,
                value: PluginState,
                oldState: EditorState
            ): PluginState {
                const { isOpen, openOnClick } = transaction.getMeta(LINK_FORM_PLUGIN_KEY) || {};
                const state = LINK_FORM_PLUGIN_KEY.getState(oldState);

                if (typeof isOpen === 'boolean') {
                    return {
                        isOpen,
                        openOnClick
                    };
                }

                // keep the old state in case we do not receive a new one.
                return state || value;
            }
        },
        props: {
            handleDOMEvents: {
                mousedown(view, event) {
                    const editor = options.editor;
                    const pos = getPosAtDocCoords(view, event);
                    const { isOpen, openOnClick } = LINK_FORM_PLUGIN_KEY.getState(editor.state);

                    // Prevent to open the bubble Menu
                    // After closing the link form on click.
                    if (isOpen && openOnClick) {
                        editor.chain().unsetHighlight().setTextSelection(pos).run();
                    }
                }
            },
            handleClickOn(view: EditorView, pos: number, node) {
                const editor = options.editor;

                if (!editor.isActive('link') || !pos) {
                    lastNode = node;

                    return null;
                }

                // If we click again in the same link node,
                // We close the form and enable editing.
                if (isEqual(lastNode, node)) {
                    editor.chain().setTextSelection(pos).closeLinkForm().run();

                    return null;
                }

                openFormLinkOnclik({ editor, view, pos });
                lastNode = node;

                return true;
            },

            handleDoubleClickOn(view: EditorView, pos: number) {
                const editor = options.editor;

                // same node here
                if (!editor.isActive('link')) {
                    return null;
                }

                openFormLinkOnclik({ editor, view, pos });

                return true;
            }
        }
    });
};
