import { PluginKey } from 'prosemirror-state';
import tippy, { GetReferenceClientRect, Instance, Props } from 'tippy.js';

import { ComponentRef, ViewContainerRef } from '@angular/core';

import { Editor } from '@tiptap/core';
import BubbleMenu from '@tiptap/extension-bubble-menu';

import { EditorAssetTypes } from '@dotcms/dotcms-models';

import { AssetTabviewFormComponent } from './asset-tabview-form.component';
import { bubbleAssetTabviewFormPlugin } from './plugins/bubble-asset-tabview-form.plugin';

export const BUBBLE_IMAGE_TABVIEW_FORM_PLUGIN_KEY = new PluginKey('bubble-image-form');

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        AssetTabviewForm: {
            openAssetForm: (asset?: EditorAssetTypes) => ReturnType;
            closeAssetForm: () => ReturnType;
            setMedia: (mediaType: EditorAssetTypes, payload) => ReturnType;
        };
    }
}

const tippyOptions: Partial<Props> = {
    interactive: true,
    duration: 0,
    maxWidth: 'none',
    trigger: 'manual',
    placement: 'bottom-start',
    hideOnClick: 'toggle',
    popperOptions: {
        modifiers: [
            {
                name: 'flip',
                options: { fallbackPlacements: ['top-start'] }
            }
        ]
    }
};

interface StartProps {
    editor: Editor;
    asset: EditorAssetTypes;
    getPosition: GetReferenceClientRect;
}

export interface RenderProps {
    onStart: (value: StartProps) => void;
    onHide: (editor: Editor) => void;
    onDestroy: () => void;
}

export const BubbleAssetTabviewFormExtension = (viewContainerRef: ViewContainerRef) => {
    let formTippy: Instance | undefined;
    let component: ComponentRef<AssetTabviewFormComponent>;
    let element: Element;

    function onStart({ editor, asset, getPosition }: StartProps) {
        setUpTippy(editor);
        setUpComponent(editor, asset);

        formTippy.setProps({
            content: element,
            getReferenceClientRect: getPosition,
            onClickOutside: () => onHide(editor)
        });
        formTippy.show();
    }

    function onHide(editor): void {
        editor.commands.closeAssetForm(false);
        formTippy?.hide();
        component?.destroy();
    }

    function onDestroy() {
        formTippy?.destroy();
        component?.destroy();
    }

    function setUpTippy(editor: Editor) {
        const { element } = editor.options;
        const editorIsAttached = !!element.parentElement;

        if (formTippy || !editorIsAttached) {
            return;
        }

        formTippy = tippy(element.parentElement, tippyOptions);
    }

    function setUpComponent(editor: Editor, asset: EditorAssetTypes) {
        component = viewContainerRef.createComponent(AssetTabviewFormComponent);
        component.instance.languageId = editor.storage.dotConfig.lang;
        component.instance.assetType = asset;
        component.instance.onSelectAsset = (payload) => {
            editor.chain().setMedia(asset, payload).addNextLine().closeAssetForm().run();
        };

        element = component.location.nativeElement;
        component.changeDetectorRef.detectChanges();
    }

    return BubbleMenu.extend<unknown>({
        name: 'bubbleImageForm',

        addOptions() {
            return {
                element: null,
                tippyOptions: {},
                pluginKey: BUBBLE_IMAGE_TABVIEW_FORM_PLUGIN_KEY
            };
        },

        addCommands() {
            return {
                openAssetForm:
                    (asset) =>
                    ({ chain }) => {
                        return chain()
                            .command(({ tr }) => {
                                tr.setMeta(BUBBLE_IMAGE_TABVIEW_FORM_PLUGIN_KEY, {
                                    open: true,
                                    asset
                                });

                                return true;
                            })
                            .run();
                    },
                closeAssetForm:
                    () =>
                    ({ chain }) => {
                        return chain()
                            .command(({ tr }) => {
                                tr.setMeta(BUBBLE_IMAGE_TABVIEW_FORM_PLUGIN_KEY, { open: false });

                                return true;
                            })
                            .run();
                    },
                setMedia:
                    (mediaType, payload) =>
                    ({ chain }) => {
                        switch (mediaType) {
                            case 'video':
                                return chain().setVideo(payload).run();

                            case 'image':
                                return chain().addDotImage(payload).run();
                        }
                    }
            };
        },

        addProseMirrorPlugins() {
            return [
                bubbleAssetTabviewFormPlugin({
                    pluginKey: BUBBLE_IMAGE_TABVIEW_FORM_PLUGIN_KEY,
                    editor: this.editor,
                    render: () =>
                        ({
                            onStart,
                            onHide,
                            onDestroy
                        } as RenderProps)
                })
            ];
        }
    });
};
