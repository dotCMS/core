import { Plugin, PluginKey } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Subscription } from 'rxjs';

import { ComponentRef, Injector, ViewContainerRef } from '@angular/core';

import { take } from 'rxjs/operators';

import { Extension } from '@tiptap/core';

import { DotCMSContentlet, EditorAssetTypes } from '@dotcms/dotcms-models';

import { UploadPlaceholderComponent } from './components/upload-placeholder/upload-placeholder.component';
import { PlaceholderPlugin } from './plugins/placeholder.plugin';

import { ImageNode } from '../../nodes';
import { deselectCurrentNode, DotUploadFileService } from '../../shared';

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

export const AssetUploader = (injector: Injector, viewContainerRef: ViewContainerRef) => {
    return Extension.create({
        name: 'assetUploader',

        addProseMirrorPlugins() {
            const dotUploadFileService = injector.get(DotUploadFileService);
            const editor = this.editor;

            let subscription$: Subscription;
            let abortControler: AbortController;

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
                component.instance.cancel.subscribe(() => {
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
             * Check if the text is an image URL.
             *
             * @param {string} text
             * @return {*}  {boolean}
             */
            function isImageURL(text: string): boolean {
                return text.match(/\.(jpeg|jpg|gif|png)$/) != null;
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
             * Get position from cursor current position when pasting an image.
             *
             * @param {EditorView} view
             * @return {*}  {{ from: number; to: number }}
             */
            function getCursorPosition(view: EditorView): { from: number; to: number } {
                const { state } = view;
                const { selection } = state;
                const { ranges } = selection;
                const from = Math.min(...ranges.map((range) => range.$from.pos));
                const to = Math.max(...ranges.map((range) => range.$to.pos));

                return { from, to };
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
                            editor.commands.insertAssetAt({ type, payload: data, position });
                        },
                        (error) => alert(error.message),
                        () => removePlaceHolder(placeHolderName)
                    );
            }

            /**
             * Alert error message.
             *
             */
            function alertErrorMessage() {
                alert('Can drop just one video at a time');
            }

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
             * Check if the node is registered in the schema.
             *
             * @param {EditorAssetTypes} nodeType
             * @return {*}  {boolean}
             */
            function isNodeRegistered(nodeType: EditorAssetTypes): boolean {
                return editor.commands.isNodeRegistered(assetsNameMap[nodeType]);
            }

            return [
                PlaceholderPlugin,
                new Plugin({
                    key: new PluginKey('assetUploader'),
                    props: {
                        handleDOMEvents: {
                            click(view, event) {
                                const { doc, selection } = view.state;
                                const { ranges } = selection;
                                const from = Math.min(...ranges.map((range) => range.$from.pos));
                                const node = doc.nodeAt(from);
                                const link = (event.target as HTMLElement)?.closest('a');

                                if (link && node.type.name === ImageNode.name) {
                                    event.preventDefault();
                                }
                            },

                            paste(view, event: ClipboardEvent) {
                                const { clipboardData } = event;
                                const { files } = clipboardData;
                                const { length } = files;
                                const file = files[0];
                                const text = clipboardData.getData('Text');
                                const nodeType = getFileType(file) as EditorAssetTypes;

                                if (isImageURL(text)) {
                                    editor.commands.insertImageAt(
                                        text,
                                        getCursorPosition(view).from
                                    );

                                    return;
                                }

                                if (!isNodeRegistered(nodeType)) {
                                    return;
                                } else if (length > 1) {
                                    alertErrorMessage();

                                    return;
                                }

                                event.preventDefault();
                                event.stopPropagation();

                                const pos = getCursorPosition(view);
                                uploadAsset({ view, file, position: pos.from });
                            },

                            drop(view, event: DragEvent) {
                                const { dataTransfer } = event;
                                const { files } = dataTransfer;
                                const { length } = files;
                                const file = files[0];
                                const nodeType = getFileType(file) as EditorAssetTypes;

                                if (!isNodeRegistered(nodeType)) {
                                    return;
                                } else if (length > 1) {
                                    alertErrorMessage();

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
                        }
                    }
                })
            ];
        }
    });
};
