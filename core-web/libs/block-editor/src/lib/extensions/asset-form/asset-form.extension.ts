import { PluginKey } from 'prosemirror-state';
import tippy, { GetReferenceClientRect, Instance, Props } from 'tippy.js';

import { ComponentRef, ViewContainerRef } from '@angular/core';

import { Editor } from '@tiptap/core';
import BubbleMenu from '@tiptap/extension-bubble-menu';

import { EditorAssetTypes } from '@dotcms/dotcms-models';

import { AssetFormComponent } from './asset-form.component';
import { bubbleAssetFormPlugin } from './plugins/bubble-asset-form.plugin';

export const BUBBLE_ASSET_FORM_PLUGIN_KEY = new PluginKey('bubble-image-form');

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        AssetTabviewForm: {
            openAssetForm: (data: { type: EditorAssetTypes }) => ReturnType;
            closeAssetForm: () => ReturnType;
            insertAsset: (data: { type: EditorAssetTypes; payload }) => ReturnType;
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
    type: EditorAssetTypes;
    getPosition: GetReferenceClientRect;
}

export interface RenderProps {
    onStart: (value: StartProps) => void;
    onHide: (editor: Editor) => void;
    onDestroy: () => void;
}

export const BubbleAssetFormExtension = (viewContainerRef: ViewContainerRef) => {
    let formTippy: Instance | undefined;
    let component: ComponentRef<AssetFormComponent>;
    let element: Element;

    function onStart({ editor, type, getPosition }: StartProps) {
        setUpTippy(editor);
        setUpComponent(editor, type);

        formTippy.setProps({
            content: element,
            getReferenceClientRect: getPosition,
            onClickOutside: () => onHide(editor)
        });
        formTippy.show();
    }

    function onHide(editor): void {
        editor.commands.closeAssetForm();
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

    function setUpComponent(editor: Editor, type: EditorAssetTypes) {
        component = viewContainerRef.createComponent(AssetFormComponent);
        component.instance.languageId = editor.storage.dotConfig.lang;
        component.instance.type = type;
        component.instance.onSelectAsset = (payload) => {
            editor.chain().insertAsset({ type, payload }).addNextLine().closeAssetForm().run();
        };

        element = component.location.nativeElement;
        component.changeDetectorRef.detectChanges();
    }

    return BubbleMenu.extend<unknown>({
        name: 'bubbleAssetForm',

        addOptions() {
            return {
                element: null,
                tippyOptions: {},
                pluginKey: BUBBLE_ASSET_FORM_PLUGIN_KEY
            };
        },

        addCommands() {
            return {
                openAssetForm:
                    ({ type }) =>
                    ({ chain }) => {
                        return chain()
                            .command(({ tr }) => {
                                tr.setMeta(BUBBLE_ASSET_FORM_PLUGIN_KEY, { open: true, type });

                                return true;
                            })
                            .freezeScroll(true)
                            .run();
                    },
                closeAssetForm:
                    () =>
                    ({ chain }) => {
                        return chain()
                            .command(({ tr }) => {
                                tr.setMeta(BUBBLE_ASSET_FORM_PLUGIN_KEY, { open: false });

                                return true;
                            })
                            .freezeScroll(false)
                            .run();
                    },
                insertAsset:
                    ({ type, payload }) =>
                    ({ chain }) => {
                        switch (type) {
                            case 'video':
                                return (
                                    //This method returns true if it was able to set the youtube video
                                    chain().setYoutubeVideo({ src: payload }).run() ||
                                    chain().setVideo(payload).run()
                                );

                            case 'image':
                                return chain().addDotImage(payload).run();
                        }
                    }
            };
        },

        addProseMirrorPlugins() {
            return [
                bubbleAssetFormPlugin({
                    pluginKey: BUBBLE_ASSET_FORM_PLUGIN_KEY,
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
