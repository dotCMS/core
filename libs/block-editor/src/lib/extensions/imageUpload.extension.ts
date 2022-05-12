import { Plugin, PluginKey } from 'prosemirror-state';
import { ComponentRef, Injector, ViewContainerRef } from '@angular/core';
import { Extension } from '@tiptap/core';
import { DotImageService } from './services/dot-image/dot-image.service';
import { EditorView } from 'prosemirror-view';
import { LoaderComponent, MessageType } from './components/loader/loader.component';
import { PlaceholderPlugin } from '../plugins/placeholder.plugin';
import { take } from 'rxjs/operators';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

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
                        id: id,
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
                    .publishContent(files)
                    .pipe(take(1))
                    .subscribe(
                        (dotAssets: DotCMSContentlet[]) => {
                            const data = dotAssets[0][Object.keys(dotAssets[0])[0]];
                            const node = {
                                attrs: {
                                    data
                                },
                                type: 'dotImage'
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
                            paste(view, event) {
                                if (areImageFiles(event)) {
                                    if (event.clipboardData.files.length !== 1) {
                                        alert('Can paste just one image at a time');
                                        return false;
                                    }
                                    const { from } = getPositionFromCursor(view);
                                    const files = Array.from(event.clipboardData.files);
                                    uploadImages(view, files, from);
                                }

                                return false;
                            },

                            drop(view, event) {
                                if (areImageFiles(event)) {
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
