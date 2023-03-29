import { Plugin, PluginKey } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { ComponentRef, Injector, ViewContainerRef } from '@angular/core';

import { take } from 'rxjs/operators';

import { Extension } from '@tiptap/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { LoaderComponent, MessageType } from './components/loader/loader.component';
import { PlaceholderPlugin } from './plugins/placeholder.plugin';
import { DotImageService } from './services/dot-image/dot-image.service';

import { ImageNode } from '../../nodes';
import { deselectCurrentNode } from '../../shared';

function checkImageURL(url) {
    return url.match(/\.(jpeg|jpg|gif|png)$/) != null;
}

export const ImageUpload = (injector: Injector, viewContainerRef: ViewContainerRef) => {
    return Extension.create({
        name: 'imageUpload',

        addProseMirrorPlugins() {
            const dotImageService = injector.get(DotImageService);
            const editor = this.editor;
            function areImageFiles(event: ClipboardEvent | DragEvent): boolean {
                let files: FileList;
                if (event.type === 'drop') {
                    files = (event as DragEvent).dataTransfer.files;
                } else {
                    //paste
                    files = (event as ClipboardEvent).clipboardData.files;
                }

                if (files.length > 0) {
                    for (let i = 0; i < files.length; i++) {
                        if (!files[i].type.startsWith('image/')) {
                            return false;
                        }
                    }
                }

                return !!files.length;
            }

            function isImageBlockAllowed(): boolean {
                const allowedBlocks: string[] = editor.storage.dotConfig.allowedBlocks;

                return allowedBlocks.length > 1 ? allowedBlocks.includes('image') : true;
            }

            function setPlaceHolder(view: EditorView, position: number, id: string) {
                const loadingBlock: ComponentRef<LoaderComponent> =
                    viewContainerRef.createComponent(LoaderComponent);
                const tr = view.state.tr;
                loadingBlock.instance.data = {
                    message: 'Uploading...',
                    type: MessageType.INFO
                };
                loadingBlock.changeDetectorRef.detectChanges();

                tr.setMeta(PlaceholderPlugin, {
                    add: {
                        id,
                        pos: position,
                        element: loadingBlock.location.nativeElement
                    }
                });

                view.dispatch(tr);
            }

            function uploadImages(view: EditorView, files: File[], position: number) {
                const placeHolderName = files[0].name;
                setPlaceHolder(view, position, placeHolderName);
                dotImageService
                    .publishContent({ data: files[0] })
                    .pipe(take(1))
                    .subscribe(
                        (dotAssets: DotCMSContentlet[]) => {
                            const data = dotAssets[0][Object.keys(dotAssets[0])[0]];
                            const { asset, name } = data;
                            const node = {
                                attrs: {
                                    data,
                                    src: asset,
                                    title: name,
                                    alt: name
                                },
                                type: ImageNode.name
                            };
                            editor.commands.insertContentAt(position, node);
                        },
                        (error) => alert(error.message),
                        () => removePlaceHolder(placeHolderName)
                    );
            }

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
            function getPositionFromCursor(view: EditorView): { from: number; to: number } {
                const { state } = view;
                const { selection } = state;
                const { ranges } = selection;
                const from = Math.min(...ranges.map((range) => range.$from.pos));
                const to = Math.max(...ranges.map((range) => range.$to.pos));

                return { from, to };
            }

            return [
                PlaceholderPlugin,
                new Plugin({
                    key: new PluginKey('imageUpload'),
                    props: {
                        handleDOMEvents: {
                            // Avoid opening a image link on `click` in `dev` mode.
                            click(view, event) {
                                const { doc, selection } = view.state;
                                const { ranges } = selection;
                                const from = Math.min(...ranges.map((range) => range.$from.pos));
                                const node = doc.nodeAt(from);
                                const link = (event.target as HTMLElement)?.closest('a');

                                if (link && node.type.name === ImageNode.name) {
                                    event.preventDefault();

                                    return true;
                                }

                                return true;
                            },
                            paste(view, event: ClipboardEvent) {
                                if (!isImageBlockAllowed()) {
                                    return true;
                                }

                                const url = event.clipboardData.getData('Text');
                                const { from } = getPositionFromCursor(view);

                                if (areImageFiles(event)) {
                                    // Avoid tiptap image extension default behavior on paste.
                                    event.preventDefault();
                                    if (event.clipboardData.files.length !== 1) {
                                        alert('Can paste just one image at a time');

                                        return true;
                                    }

                                    const files = Array.from(event.clipboardData.files);
                                    uploadImages(view, files, from);
                                } else if (checkImageURL(url)) {
                                    const node = {
                                        attrs: {
                                            src: url
                                        },
                                        type: ImageNode.name
                                    };
                                    editor.commands.insertContentAt(from, node);
                                }
                            },
                            drop(view, event: DragEvent) {
                                if (isImageBlockAllowed() && areImageFiles(event)) {
                                    event.preventDefault();
                                    if (event.dataTransfer.files.length !== 1) {
                                        alert('Can drop just one image at a time');

                                        return false;
                                    }

                                    const { pos: position } = view.posAtCoords({
                                        left: event.clientX,
                                        top: event.clientY
                                    });

                                    const files = Array.from(event.dataTransfer.files);
                                    uploadImages(view, files, position);
                                }

                                return false;
                            }
                        }
                    }
                })
            ];
        }
    });
};
