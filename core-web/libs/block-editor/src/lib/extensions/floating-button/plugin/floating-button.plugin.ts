import { EditorState, Plugin, PluginKey } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Subject } from 'rxjs';
import tippy, { Instance } from 'tippy.js';

import { ComponentRef } from '@angular/core';

import { take, takeUntil, tap } from 'rxjs/operators';

import { Editor } from '@tiptap/core';

import { DotUploadFileService, FileStatus } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { ImageNode } from '../../../nodes';
import { getNodeCoords } from '../../../shared';
import { FloatingButtonComponent } from '../floating-button.component';

export const setCoords = ({ viewCoords, nodeCoords }): DOMRect => {
    const offset = 65;
    const { bottom: nodeBottom, left, top } = nodeCoords;
    const { bottom: viewBottom } = viewCoords;
    const isBottomOverflow = Math.ceil(viewBottom - nodeBottom) < 0;
    const newTop = isBottomOverflow ? viewBottom : top - offset;
    // Is the top image is lower than editor button, then use image top.
    const pos = top < viewBottom ? newTop : top + offset;

    return {
        ...nodeCoords.toJSON(),
        top: pos,
        left
    };
};

export const DotFloatingButtonPlugin = (options) => {
    return new Plugin({
        key: options.pluginKey as PluginKey,
        view: (view) => new DotFloatingButtonPluginView({ view, ...options })
    });
};

export class DotFloatingButtonPluginView {
    private editor: Editor;
    private element: HTMLElement;
    private component: ComponentRef<FloatingButtonComponent>;
    private view: EditorView;
    private tippy: Instance | undefined;
    private preventHide = false;
    private $destroy = new Subject<boolean>();
    private dotUploadFileService: DotUploadFileService;
    private imageUrl: string;
    private initialLabel = 'Import to dotCMS';
    private offset = 10;

    /* @Overrrider */
    constructor({ dotUploadFileService, editor, component, element, view }) {
        this.editor = editor;
        this.element = element;
        this.view = view;
        this.component = component;
        this.dotUploadFileService = dotUploadFileService;

        this.component.instance.byClick
            .pipe(takeUntil(this.$destroy))
            .subscribe(() => this.uploadImagedotCMS());
        // Detaches menu content from its current parent
        this.element.remove();
        this.element.style.visibility = 'visible';
    }

    /* @Overrrider */
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

        this.createTooltip();

        this.tippy?.setProps({
            maxWidth: image?.width - this.offset || 250,
            getReferenceClientRect: () => {
                const viewCoords = view.dom.parentElement.getBoundingClientRect();
                const nodeCoords = getNodeCoords(node, type);

                return setCoords({ viewCoords, nodeCoords });
            }
        });

        this.show();
    }

    createTooltip() {
        const { element: editorElement } = this.editor.options;
        const editorIsAttached = !!editorElement.parentElement;

        if (this.tippy || !editorIsAttached) {
            return;
        }

        this.tippy = tippy(editorElement, {
            maxWidth: 'none',
            duration: 500,
            getReferenceClientRect: null,
            content: this.element,
            interactive: true,
            trigger: 'manual',
            placement: 'bottom-end',
            offset: [-this.offset, 0],
            hideOnClick: 'toggle'
        });
    }

    show() {
        this.tippy?.show();
    }

    hide() {
        this.tippy?.hide();
    }

    /* @Overrrider */
    destroy() {
        this.tippy?.destroy();
        this.component.destroy();
        this.$destroy.next(true);
        this.$destroy.complete();
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
        this.component.instance.label = label;
        this.component.changeDetectorRef.detectChanges();
    }

    private updateButtonLoading(isLoading: boolean) {
        this.component.instance.isLoading = isLoading;
        this.component.changeDetectorRef.detectChanges();
    }
}
