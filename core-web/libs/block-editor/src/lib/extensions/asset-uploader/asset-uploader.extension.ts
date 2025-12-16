import { Plugin, PluginKey } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Subscription } from 'rxjs';

import { ComponentRef, Injector, ViewContainerRef } from '@angular/core';

import { take } from 'rxjs/operators';

import { Extension } from '@tiptap/core';

import { DotUploadFileService } from '@dotcms/data-access';
import { DotCMSContentlet, EditorAssetTypes } from '@dotcms/dotcms-models';

import { UploadPlaceholderComponent } from './components/upload-placeholder/upload-placeholder.component';
import { PlaceholderPlugin } from './plugins/placeholder.plugin';

import { ImageNode } from '../../nodes';
import { deselectCurrentNode, getCursorPosition, isImageURL } from '../../shared';

interface UploadNode {
    view: EditorView;
    file: File;
    position: number;
}

const assetsNameMap = {
    video: 'dotVideo',
    image: 'dotImage'
};

interface PlaceHolderProps {
    view: EditorView;
    position: number;
    id: string;
    type: EditorAssetTypes;
}

/**
 * Asset Uploader Extension.
 *
 * @param {Injector} injector
 * @param {ViewContainerRef} viewContainerRef
 * @return {*}
 */
export const AssetUploader = (injector: Injector, viewContainerRef: ViewContainerRef) => {
    return Extension.create({
        name: 'assetUploader',

        addProseMirrorPlugins() {
            const dotUploadFileService = injector.get(DotUploadFileService);
            const editor = this.editor;

            let subscription$: Subscription;
            let abortControler: AbortController;

            /**
             * Get file type.
             *
             * @param {File} file
             * @return {*}  {string}
             */
            function getFileType(file: File): string {
                return file?.type.split('/')[0] || '';
            }

            /**
             * Alert error message when the user try to drop more than one asset.
             *
             */
            function alertErrorMessage(type: EditorAssetTypes) {
                alert(`Can drop just one ${type} at a time`);
            }

            /**
             * Upload file to the server.
             *
             * @param {EditorView} view
             * @param {number} position
             * @param {string} id
             */
            function insertPlaceHolder({ view, position, id, type }: PlaceHolderProps) {
                const component: ComponentRef<UploadPlaceholderComponent> =
                    viewContainerRef.createComponent(UploadPlaceholderComponent);
                const tr = view.state.tr;

                component.instance.type = type;
                component.instance.canceled.subscribe(() => {
                    removePlaceHolder(id);
                    abortControler.abort();
                    subscription$.unsubscribe();
                });

                component.changeDetectorRef.detectChanges();

                tr.setMeta(PlaceholderPlugin, {
                    add: {
                        id,
                        pos: position,
                        element: component.location.nativeElement
                    }
                });

                view.dispatch(tr);
            }

            /**
             * Remove placeholder from the editor.
             *
             * @param {string} id
             */
            function removePlaceHolder(id: string) {
                const { view } = editor;
                const { state } = view;
                view.dispatch(
                    state.tr.setMeta(PlaceholderPlugin, {
                        remove: { id }
                    })
                );
                deselectCurrentNode(view);
            }

            /**
             * Upload asset and insert it in the editor when the upload is done.
             *
             * @param {UploadNode} { view, file, position }
             */
            function uploadAsset({ view, file, position }: UploadNode) {
                const type = getFileType(file) as EditorAssetTypes;
                const placeHolderName = file.name;
                insertPlaceHolder({ view, position, id: placeHolderName, type });

                abortControler = new AbortController();
                const { signal } = abortControler;

                subscription$ = dotUploadFileService
                    .publishContent({ data: file, signal })
                    .pipe(take(1))
                    .subscribe(
                        (dotAssets: DotCMSContentlet[]) => {
                            const data = dotAssets[0][Object.keys(dotAssets[0])[0]];
                            editor.commands.insertAsset({ type, payload: data, position });
                        },
                        (error) => alert(error.message),
                        () => removePlaceHolder(placeHolderName)
                    );
            }

            /**
             * Check if the node is registered in the schema.
             *
             * @param {EditorAssetTypes} nodeType
             * @return {*}  {boolean}
             */
            function isNodeRegistered(nodeType: EditorAssetTypes): boolean {
                return editor.commands.isNodeRegistered(assetsNameMap[nodeType]);
            }

            /**
             * Prevent open image link on click in Dev Mode.
             *
             * @param {EditorView} view
             * @param {MouseEvent} event
             * @return {*}
             */
            function hanlderClick(view: EditorView, event: MouseEvent) {
                const { doc, selection } = view.state;
                const { ranges } = selection;
                const from = Math.min(...ranges.map((range) => range.$from.pos));
                const node = doc.nodeAt(from);
                const link = (event.target as HTMLElement)?.closest('a');

                if (link && node.type.name === ImageNode.name) {
                    event.preventDefault();
                    event.stopPropagation();

                    return true;
                }

                return false;
            }

            /**
             * Handle Paste event.
             *
             * @param {EditorView} view
             * @param {ClipboardEvent} event
             * @return {*}
             */
            function hanlderPaste(view: EditorView, event: ClipboardEvent) {
                const { clipboardData } = event;
                const { files } = clipboardData;
                const text = clipboardData.getData('Text') || '';
                const type = getFileType(files[0]) as EditorAssetTypes;
                const isRegistered = isNodeRegistered(type);

                if (type && !isRegistered) {
                    alertErrorMessage(type);

                    return;
                }

                // If the text is not an image URL, we don't want to prevent the default behavior.
                // This allows the user to paste text normally. Nedeed 'cause when you copy and paste
                // text from Word, the clipboard data event is receiving an image because Word includes
                // formatting information along with the text.
                if (text && !isImageURL(text)) {
                    return;
                }

                const { from } = getCursorPosition(view);

                if (isImageURL(text) && isNodeRegistered('image')) {
                    editor.chain().insertImage(text, from).addNextLine().run();
                } else {
                    const file = files[0];
                    uploadAsset({ view, file, position: from });
                }

                event.preventDefault();
                event.stopPropagation();
            }

            /**
             * Handle Drop event.
             *
             * @param {EditorView} view
             * @param {DragEvent} event
             * @return {*}
             */
            function hanlderDrop(view: EditorView, event: DragEvent) {
                const { files } = event.dataTransfer;
                const { length } = files;
                const file = files[0];
                const type = getFileType(file) as EditorAssetTypes;

                if (!isNodeRegistered(type)) {
                    return;
                }

                if (length > 1) {
                    alertErrorMessage(type);

                    return;
                }

                event.preventDefault();
                event.stopPropagation();
                const { clientX, clientY } = event;
                const { pos } = view.posAtCoords({
                    left: clientX,
                    top: clientY
                });

                uploadAsset({ view, file, position: pos });
            }

            return [
                PlaceholderPlugin,
                new Plugin({
                    key: new PluginKey('assetUploader'),
                    props: {
                        handleDOMEvents: {
                            click(view, event) {
                                hanlderClick(view, event);
                            },

                            paste(view, event: ClipboardEvent) {
                                hanlderPaste(view, event);
                            },

                            drop(view, event: DragEvent) {
                                hanlderDrop(view, event);
                            }
                        }
                    }
                })
            ];
        }
    });
};
