import { Plugin, PluginKey } from 'prosemirror-state';
import { ComponentFactoryResolver, ComponentRef, Injector } from '@angular/core';
import { Extension } from '@tiptap/core';
import { DotImageService } from './services/dot-image/dot-image.service';
import { EditorView } from 'prosemirror-view';
import { LoaderComponent, MessageType } from './components/loader/loader.component';
import { PlaceholderPlugin } from '../plugins/placeholder.plugin';
import { take } from 'rxjs/operators';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

export const ImageUpload = (injector: Injector, resolver: ComponentFactoryResolver) => {
    return Extension.create({
        name: 'imageUpload',

        addProseMirrorPlugins() {
            const dotImageService = injector.get(DotImageService);
            const loaderComponentFactory = resolver.resolveComponentFactory(LoaderComponent);

            function areImageFiles(event: ClipboardEvent | DragEvent): boolean {
                let files: FileList;
                if (event.type === 'drop') {
                    files = (event as DragEvent).dataTransfer.files;
                } else {
                    //paste
                    files = (event as ClipboardEvent).clipboardData.files;
                }
                if (!!files.length) {
                    for (let i = 0; i < files.length; i++) {
                        if (!files[i].type.startsWith('image/')) {
                            return false;
                        }
                    }
                }

                return !!files.length;
            }

            function findPlaceholder(state, id): number {
                const decorations = PlaceholderPlugin.getState(state);
                const found = decorations.find(null, null, (spec) => spec.id == id);
                return found.length ? found[0].from : null;
            }

            function setPlaceHolder(view: EditorView, position: number, id: string) {
                const loadingBlock: ComponentRef<LoaderComponent> = loaderComponentFactory.create(
                    injector
                );
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

            function uploadImages(view: EditorView, files: File[], position = 0) {
                const { schema } = view.state;

                setPlaceHolder(view, position, files[0].name);

                dotImageService
                    .publishContent(files)
                    .pipe(take(1))
                    .subscribe(
                        (dotAssets: DotCMSContentlet[]) => {
                            const tr = view.state.tr;
                            const data = dotAssets[0][Object.keys(dotAssets[0])[0]];
                            const pos = findPlaceholder(view.state, data.name);
                            const imageNode = schema.nodes.dotImage.create({
                                data: data
                            });
                            view.dispatch(
                                tr.replaceWith(pos, pos, imageNode).setMeta(PlaceholderPlugin, {
                                    remove: { id: data.name }
                                })
                            );
                        },
                        (error) => {
                            alert(error.message);
                            view.dispatch(
                                view.state.tr.setMeta(PlaceholderPlugin, {
                                    remove: { id: files[0].name }
                                })
                            );
                        }
                    );
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
                                    event.preventDefault();
                                    uploadImages(
                                        view,
                                        Array.from(event.clipboardData.files),
                                        view.state.selection.to
                                    );
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
                                    const { pos } = view.posAtCoords({
                                        left: event.clientX,
                                        top: event.clientY
                                    });
                                    uploadImages(view, Array.from(event.dataTransfer.files), pos);
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
