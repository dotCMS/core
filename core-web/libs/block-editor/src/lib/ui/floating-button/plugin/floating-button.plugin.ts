import { computePosition, flip, offset, shift } from '@floating-ui/dom';
import { EditorState, Plugin, PluginKey } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Subject } from 'rxjs';

import { ComponentRef } from '@angular/core';

import { take, takeUntil, tap } from 'rxjs/operators';

import { Editor } from '@tiptap/core';

import { DotUploadFileService, FileStatus } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { ImageNode } from '../../../nodes';
import { FloatingButtonComponent } from '../floating-button.component';

/** Configuration accepted by {@link DotFloatingButtonPlugin}. */
export interface DotFloatingButtonPluginOptions {
    /** Unique key for this ProseMirror plugin instance. */
    pluginKey: PluginKey | string;
    /** The Tiptap editor instance. */
    editor: Editor;
    /** Root DOM element of the floating button component. */
    element: HTMLElement;
    /** Angular `ComponentRef` for the floating button -- used to push inputs via `setInput()`. */
    component: ComponentRef<FloatingButtonComponent>;
    /** Service for uploading external images into dotCMS. */
    dotUploadFileService: DotUploadFileService;
}

/**
 * Creates a ProseMirror plugin that shows an "Import to dotCMS" floating button
 * over selected external images (images without a `data` attribute).
 *
 * Positioning uses `@floating-ui/dom` `computePosition()` with absolute strategy,
 * appending the element inside the editor's parent (not `document.body`).
 *
 * @param options - Plugin configuration including editor, element, and services.
 * @returns A ProseMirror `Plugin` instance.
 */
export const DotFloatingButtonPlugin = (options: DotFloatingButtonPluginOptions) => {
    return new Plugin({
        key:
            typeof options.pluginKey === 'string'
                ? new PluginKey(options.pluginKey)
                : options.pluginKey,
        view: (view) => new DotFloatingButtonPluginView({ view, ...options })
    });
};

/**
 * ProseMirror plugin view that manages visibility and positioning of the
 * floating "Import to dotCMS" button.
 *
 * Shown when the user selects an external image (one without a `data` attribute).
 * Clicking the button uploads the image to dotCMS via {@link DotUploadFileService}
 * and updates the `ImageNode` attributes with the resulting contentlet data.
 *
 * Positioning is handled by Floating UI's `computePosition()` with `flip` and
 * `shift` middleware, placed `bottom-end` relative to the image element.
 */
export class DotFloatingButtonPluginView {
    private editor: Editor;
    private element: HTMLElement;
    private component: ComponentRef<FloatingButtonComponent>;
    private view: EditorView;
    private dotUploadFileService: DotUploadFileService;
    private imageUrl: string;
    private isVisible = false;
    private preventHide = false;
    private $destroy = new Subject<boolean>();

    private readonly initialLabel = 'Import to dotCMS';
    private readonly floatingOffset = 10;

    constructor({
        dotUploadFileService,
        editor,
        component,
        element,
        view
    }: DotFloatingButtonPluginOptions & { view: EditorView }) {
        this.editor = editor;
        this.element = element;
        this.view = view;
        this.component = component;
        this.dotUploadFileService = dotUploadFileService;

        this.component.instance.byClick.subscribe(() => this.uploadImagedotCMS());

        this.element.remove();
        this.element.style.visibility = 'visible';
        this.element.style.position = 'absolute';
    }

    /**
     * Called by ProseMirror on every transaction. Shows the button when an
     * external image is selected; hides it otherwise.
     */
    update(view: EditorView, oldState?: EditorState) {
        const { state, composing } = view;
        const { doc, selection } = state;
        const { empty, ranges } = selection;

        const isSame = oldState && oldState.doc.eq(doc) && oldState.selection.eq(selection);

        if (composing || isSame || this.preventHide) {
            this.preventHide = false;

            return;
        }

        const from = Math.min(...ranges.map((range) => range.$from.pos));
        const type = doc.nodeAt(from)?.type.name;
        const props = this.editor.getAttributes(ImageNode.name);
        const isImage = type === ImageNode.name;

        if (empty || !isImage || props?.data) {
            this.hide();

            return;
        }

        const node = view.nodeDOM(from) as HTMLElement;
        const image = node.querySelector('img');

        this.imageUrl = props?.src;
        this.updateButtonLabel(this.initialLabel);

        this.updatePosition(node, image);
        this.show();
    }

    /** Hides the element, destroys the Angular component, and completes the teardown Subject. */
    destroy() {
        this.hide();
        this.component.destroy();
        this.$destroy.next(true);
        this.$destroy.complete();
    }

    /**
     * Positions the floating button relative to the image using Floating UI.
     * Falls back to the node's first child or the node itself when no `<img>` is found.
     */
    private updatePosition(nodeElement: HTMLElement, imgElement: HTMLElement | null) {
        const referenceEl = imgElement || nodeElement.firstElementChild || nodeElement;

        const virtualElement = {
            getBoundingClientRect: () => (referenceEl as HTMLElement).getBoundingClientRect()
        };

        const maxWidth = imgElement ? imgElement.offsetWidth - this.floatingOffset : 250;

        this.element.style.maxWidth = `${maxWidth}px`;

        computePosition(virtualElement as Element, this.element, {
            placement: 'bottom-end',
            strategy: 'absolute',
            middleware: [
                offset({ mainAxis: 0, crossAxis: -this.floatingOffset }),
                flip({ fallbackPlacements: ['top-end'] }),
                shift({ padding: 8 })
            ]
        }).then(({ x, y, strategy }) => {
            this.element.style.position = strategy;
            this.element.style.left = `${x}px`;
            this.element.style.top = `${y}px`;
        });
    }

    private show() {
        if (this.isVisible) {
            return;
        }

        this.element.style.visibility = 'visible';
        this.element.style.opacity = '1';

        const editorParent = this.view.dom.parentElement;
        editorParent?.appendChild(this.element);

        this.isVisible = true;
    }

    private hide() {
        if (!this.isVisible) {
            return;
        }

        this.element.style.visibility = 'hidden';
        this.element.style.opacity = '0';
        this.element.remove();

        this.isVisible = false;
    }

    private updateImageNode(data: DotCMSContentlet): void {
        const { fileAsset, asset } = data;
        this.setPreventHide();
        const attr = this.editor.getAttributes(ImageNode.name);
        this.editor.commands.updateAttributes(ImageNode.name, {
            ...attr,
            src: fileAsset || asset,
            data
        });
    }

    private setPreventHide(): void {
        this.preventHide = true;
    }

    private uploadImagedotCMS() {
        this.updateButtonLoading(true);
        this.dotUploadFileService
            .publishContent({
                data: this.imageUrl,
                statusCallback: (status: string) => this.updateButtonLabel(status)
            })
            .pipe(
                take(1),
                takeUntil(this.$destroy),
                tap(() => this.updateButtonLoading(false))
            )
            .subscribe(
                (data) => {
                    const contentlet = data[0];
                    this.updateButtonLabel(FileStatus.COMPLETED);
                    this.updateImageNode(contentlet[Object.keys(contentlet)[0]]);
                },
                () => {
                    this.updateButtonLoading(false);
                    this.updateButtonLabel(FileStatus.ERROR);
                    this.setPreventHide();
                }
            );
    }

    private updateButtonLabel(label: string) {
        this.component.setInput('label', label);
    }

    private updateButtonLoading(isLoading: boolean) {
        this.component.setInput('isLoading', isLoading);
    }
}
