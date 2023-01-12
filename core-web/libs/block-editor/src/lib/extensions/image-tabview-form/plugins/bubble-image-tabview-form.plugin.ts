import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Subject } from 'rxjs';
import tippy, { Instance, Props } from 'tippy.js';

import { ComponentRef } from '@angular/core';

import { Editor, posToDOMRect } from '@tiptap/core';

import { ImageNode } from '@dotcms/block-editor';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { ImageTabviewFormComponent } from '../image-tabview-form.component';

interface PluginState {
    open: boolean;
}

export interface BubbleLinkFormProps {
    pluginKey: PluginKey;
    editor: Editor;
    element: HTMLElement;
    tippyOptions?: Partial<Props>;
    component?: ComponentRef<ImageTabviewFormComponent>;
}

export type BubbleImageTabFormViewProps = BubbleLinkFormProps & {
    view: EditorView;
};

export class BubbleImageTabFormView {
    public editor: Editor;

    public element: HTMLElement;

    public view: EditorView;

    public tippy: Instance | undefined;

    public tippyOptions?: Partial<Props>;

    public pluginKey: PluginKey;

    public component?: ComponentRef<ImageTabviewFormComponent>;

    private $destroy = new Subject<boolean>();

    constructor({
        editor,
        element,
        view,
        tippyOptions = {},
        pluginKey,
        component
    }: BubbleImageTabFormViewProps) {
        this.editor = editor;
        this.element = element;
        this.view = view;

        this.tippyOptions = tippyOptions;

        // Detaches menu content from its current parent
        this.element.remove();
        this.element.style.visibility = 'visible';
        this.pluginKey = pluginKey;
        this.component = component;

        // Set Component Inputs
        this.component.instance.languageId = this.editor.storage.dotConfig.lang;
        this.component.instance.selectItemCallback = this.addImage.bind(this);

        this.editor.on('focus', () => {
            if (this.tippy?.state.isShown) {
                this.hide();
            }
        });
    }

    update(view: EditorView, prevState?: EditorState): void {
        const next = this.pluginKey?.getState(view.state);
        const prev = prevState ? this.pluginKey?.getState(prevState) : { open: false };

        const { state } = view;
        const { selection } = state;

        if (next?.open === prev?.open || !next?.open) {
            return;
        }

        this.createTooltip();

        this.tippy?.setProps({
            getReferenceClientRect: () => {
                const { from, to } = selection;

                return posToDOMRect(view, from, to);
            }
        });

        this.show();
        this.component.changeDetectorRef.detectChanges();
    }

    createTooltip() {
        const { element: editorElement } = this.editor.options;
        const editorIsAttached = !!editorElement.parentElement;

        if (this.tippy || !editorIsAttached) {
            return;
        }

        this.tippy = tippy(editorElement.parentElement, {
            ...this.tippyOptions,
            duration: 0,
            content: this.element,
            interactive: true,
            maxWidth: 'none',
            trigger: 'manual',
            placement: 'bottom-start',
            hideOnClick: 'toggle',
            onClickOutside: () => this.hide(),
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

    addImage(contentlet: DotCMSContentlet) {
        const { selection } = this.editor.state;
        const { title, asset, fileAsset } = contentlet;
        const node = {
            attrs: {
                data: contentlet,
                src: fileAsset || asset,
                title,
                alt: title
            },
            type: ImageNode.name
        };
        this.editor.chain().insertContentAt(selection.head, node).addNextLine().run();
        this.hide();
    }

    closeForm() {
        const transaction = this.editor.state.tr.setMeta(this.pluginKey, {
            open: false
        });
        this.editor.view.dispatch(transaction);
        this.component.instance.resetForm();
    }

    show() {
        this.tippy?.show();
        this.component.instance.setLoading(true);
        this.component.instance.offset$.next(0);
        requestAnimationFrame(() => this.component.instance.inputSearch.nativeElement.focus());
    }

    hide() {
        this.tippy?.hide();
        this.closeForm();
        this.editor.view.focus();
        this.component.changeDetectorRef.detectChanges();
    }

    destroy() {
        this.tippy?.destroy();
        this.$destroy.next(true);
        this.component?.destroy();
    }
}

export const bubbleImageTabviewFormPlugin = (options: BubbleLinkFormProps) => {
    return new Plugin({
        key: options.pluginKey as PluginKey,
        view: (view) => new BubbleImageTabFormView({ view, ...options }),
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
                const { open } = transaction.getMeta(options.pluginKey) || {};
                const state = options.pluginKey?.getState(oldState);

                if (typeof open === 'boolean') {
                    return { open };
                }

                // keep the old state in case we do not receive a new one.
                return state || value;
            }
        }
    });
};
