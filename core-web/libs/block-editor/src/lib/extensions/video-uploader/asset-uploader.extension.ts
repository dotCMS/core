import { Plugin, PluginKey } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Subscription } from 'rxjs';

import { ComponentRef, Injector, ViewContainerRef } from '@angular/core';

import { take } from 'rxjs/operators';

import { Extension } from '@tiptap/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { VideoPlaceholderComponent } from './components/video-placeholder/video-placeholder.component';

import { VideoNode } from '../../nodes';
import { deselectCurrentNode, DotUploadFileService } from '../../shared';
import { PlaceholderPlugin } from '../image-uploader/plugins/placeholder.plugin';

interface UploadNode {
    view: EditorView;
    file: File;
    position: number;
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
            function insertPlaceHolder(view: EditorView, position: number, id: string) {
                const component: ComponentRef<VideoPlaceholderComponent> =
                    viewContainerRef.createComponent(VideoPlaceholderComponent);
                const tr = view.state.tr;

                component.instance.cancel.subscribe(() => {
                    removePlaceHolder(id);
                    abortControler.abort();
                    subscription$.unsubscribe();
                });

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
                const placeHolderName = file.name;
                insertPlaceHolder(view, position, placeHolderName);

                abortControler = new AbortController();
                const { signal } = abortControler;

                subscription$ = dotUploadFileService
                    .publishContent({ data: file, signal })
                    .pipe(take(1))
                    .subscribe(
                        (dotAssets: DotCMSContentlet[]) => {
                            const data = dotAssets[0][Object.keys(dotAssets[0])[0]];
                            editor.commands.insertVideoAt(data, position);
                        },
                        (error) => alert(error.message),
                        () => removePlaceHolder(placeHolderName)
                    );
            }

            function alertErrorMessage() {
                alert('Can drop just one video at a time');
            }

            return [
                new Plugin({
                    key: new PluginKey('assetUploader'),
                    props: {
                        handleDOMEvents: {
                            paste(view, event: ClipboardEvent) {
                                if (!editor.commands.isNodeRegistered(VideoNode.name)) {
                                    return;
                                }

                                const { clipboardData } = event;
                                const { files } = clipboardData;
                                const { length } = files;

                                if (length === 1) {
                                    const file = files[0];
                                    const { type } = file;

                                    if (type.startsWith('video/')) {
                                        event.preventDefault();
                                        event.stopPropagation();

                                        const pos = getCursorPosition(view);
                                        uploadAsset({ view, file, position: pos.from });
                                    }
                                } else {
                                    alertErrorMessage();
                                }
                            },

                            drop(view, event: DragEvent) {
                                if (!editor.commands.isNodeRegistered(VideoNode.name)) {
                                    return;
                                }

                                const { dataTransfer } = event;
                                const { files } = dataTransfer;
                                const { length } = files;

                                if (length === 1) {
                                    const file = files[0];
                                    const { type } = file;

                                    if (type.startsWith('video/')) {
                                        event.preventDefault();
                                        event.stopPropagation();

                                        const { clientX, clientY } = event;
                                        const { pos } = view.posAtCoords({
                                            left: clientX,
                                            top: clientY
                                        });
                                        uploadAsset({ view, file, position: pos });
                                    }
                                } else {
                                    alertErrorMessage();
                                }
                            }
                        }
                    }
                })
            ];
        }
    });
};
