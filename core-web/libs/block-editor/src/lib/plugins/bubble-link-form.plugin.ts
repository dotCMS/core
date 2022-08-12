import { ComponentRef } from '@angular/core';
import { Subject } from 'rxjs';
import { Editor, posToDOMRect } from '@tiptap/core';
import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import tippy, { Instance, Props } from 'tippy.js';

import {
    BubbleMenuLinkFormComponent,
    NodeProps
} from '../extensions/components/bubble-menu-link-form/bubble-menu-link-form.component';

// Interface
import { LINK_FORM_PLUGIN_KEY } from '../extensions/bubble-link-form.extension';

// Utils
import { getPosAtDocCoords } from '../utils/prosemirror.utils';
import { isValidURL } from '../utils/bubble-menu.utils';
import { openFormLinkOnclik } from '../extensions/components/bubble-menu-link-form/utils/index';

import isEqual from 'lodash.isequal';
import { takeUntil } from 'rxjs/operators';

interface PluginState {
    isOpen: boolean;
    openOnClick: boolean;
}

export interface BubbleLinkFormProps {
    pluginKey: PluginKey;
    editor: Editor;
    element: HTMLElement;
    tippyOptions?: Partial<Props>;
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
            duration: 250,
            getReferenceClientRect: () => this.setTippyPosition(),
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
        const isNodeImage = node?.type.name === 'dotImage';

        // If there is an overflow, use bubble menu position as a reference.
        return isOverflow || isNodeImage ? domRect : nodeClientRect;
    }

    setLinkValues({ link, blank = false }) {
        if (link.length > 0) {
            this.isDotImageNode()
                ? this.editor.commands.setImageLink({ href: link })
                : this.editor.commands.setLink({ href: link, target: blank ? '_blank' : '_top' });
        }
    }

    removeLink() {
        this.isDotImageNode()
            ? this.editor.commands.unsetImageLink()
            : this.editor.commands.unsetLink();
        this.hide();
    }

    setInputValues() {
        const values = this.getLinkProps();
        this.component.instance.initialValues = values;
        this.component.instance.setFormValue(values);
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
    }

    getLinkProps(): NodeProps {
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
        const { type } = this.editor.state.doc.nodeAt(this.editor.state.selection.from) || {};

        return type?.name === 'dotImage';
    }

    destroy() {
        this.tippy?.destroy();
        this.editor.off('focus', this.focusHandler);
        this.$destroy.next(true);
        this.component.destroy();
    }

    private hanlderScroll(e: Event) {
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

                    return;
                }

                // If we click again in the same link node,
                // We close the form and enable editing.
                if (isEqual(lastNode, node)) {
                    editor.chain().setTextSelection(pos).closeLinkForm().run();

                    return;
                }

                openFormLinkOnclik({ editor, view, pos });
                lastNode = node;

                return true;
            },

            handleDoubleClickOn(view: EditorView, pos: number) {
                const editor = options.editor;

                // same node here
                if (!editor.isActive('link')) {
                    return;
                }

                openFormLinkOnclik({ editor, view, pos });

                return true;
            }
        }
    });
};
