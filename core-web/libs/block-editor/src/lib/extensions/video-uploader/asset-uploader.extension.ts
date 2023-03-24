import { Plugin, PluginKey } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Subscription } from 'rxjs';

import { ComponentRef, Injector, ViewContainerRef } from '@angular/core';

import { take } from 'rxjs/operators';

import { Extension } from '@tiptap/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { VideoPlaceholderComponent } from './components/video-placeholder/video-placeholder.component';

import { VideoNode } from '../../nodes';
import { deselectCurrentNode } from '../../shared';
import { PlaceholderPlugin } from '../image-uploader/plugins/placeholder.plugin';
import { DotImageService } from '../image-uploader/services/dot-image/dot-image.service';

interface UploadNode {
    view: EditorView;
    file: File;
    position: number;
}

export const AssetUploader = (injector: Injector, viewContainerRef: ViewContainerRef) => {
    return Extension.create({
        name: 'assetUploader',

        addProseMirrorPlugins() {
            let subscription$: Subscription;
            // let abortControler: AbortController;
            const dotImageService = injector.get(DotImageService);
            const editor = this.editor;

            // Move to a Command
            function insertPlaceHolder(view: EditorView, position: number, id: string) {
                const component: ComponentRef<VideoPlaceholderComponent> =
                    viewContainerRef.createComponent(VideoPlaceholderComponent);
                const tr = view.state.tr;

                component.instance.cancel.subscribe(() => {
                    removePlaceHolder(id);
                    subscription$.unsubscribe();
                });

                // VideoPlaceholderComponent.changeDetectorRef.detectChanges();

                tr.setMeta(PlaceholderPlugin, {
                    add: {
                        id,
                        pos: position,
                        element: component.location.nativeElement
                    }
                });

                view.dispatch(tr);
            }

            // Move to a Command
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
             * Get position from cursor current position when pasting an image
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

            function uploadAsset({ view, file, position }: UploadNode) {
                const placeHolderName = file.name;
                insertPlaceHolder(view, position, placeHolderName);

                // abortControler = new AbortController();
                // const { signal } = abortControler;

                subscription$ = dotImageService
                    .publishContent({ data: file })
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
                                }
                            }
                        }
                    }
                })
            ];
        }
    });
};

// const getCurrentEditorPosition = (view: EditorView): number => {
//     const { state } = view;
//     const { selection } = state;
//     const { ranges } = selection;
//     const from = Math.min(...ranges.map((range) => range.$from.pos));

//     return from;
// }

// const handlePasteVideo = (editor: Editor, view: EditorView, event: ClipboardEvent) => {
//     const { clipboardData } = event;
//     const { items } = clipboardData;
//     const { length } = items;
//     const video = items[0];

// }

// if (!isImageBlockAllowed()) {
//     return true;
// }

// const url = event.clipboardData.getData('Text');
// const { from } = getPositionFromCursor(view);

// if (areImageFiles(event)) {
//     // Avoid tiptap image extension default behavior on paste.
//     event.preventDefault();
//     if (event.clipboardData.files.length !== 1) {
//         alert('Can paste just one image at a time');

//         return true;
//     }

//     const files = Array.from(event.clipboardData.files);
//     uploadImages(view, files, from);
// } else if (checkImageURL(url)) {
//     const node = {
//         attrs: {
//             src: url
//         },
//         type: ImageNode.name
//     };
//     editor.commands.insertContentAt(from, node);
// }
