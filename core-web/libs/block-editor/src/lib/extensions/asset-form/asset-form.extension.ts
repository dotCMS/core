import { PluginKey } from 'prosemirror-state';

import { ComponentRef, ViewContainerRef } from '@angular/core';

import { Editor } from '@tiptap/core';
import BubbleMenu from '@tiptap/extension-bubble-menu';

import { DotCMSContentlet, EditorAssetTypes } from '@dotcms/dotcms-models';

import { AssetFormComponent } from './asset-form.component';
import { bubbleAssetFormPlugin } from './plugins/bubble-asset-form.plugin';

import { createFloatingUI, type FloatingUIInstance } from '../../shared/utils/floating-ui.utils';

export const BUBBLE_ASSET_FORM_PLUGIN_KEY = new PluginKey('bubble-image-form');

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        AssetTabviewForm: {
            openAssetForm: (data: { type: EditorAssetTypes }) => ReturnType;
            closeAssetForm: () => ReturnType;
            insertAsset: (data: {
                type: EditorAssetTypes;
                payload: string | DotCMSContentlet;
                position?: number;
            }) => ReturnType;
        };
    }
}

interface StartProps {
    editor: Editor;
    type: EditorAssetTypes;
    getPosition: () => DOMRect;
}

export interface RenderProps {
    onStart: (value: StartProps) => void;
    onHide: (editor: Editor) => void;
    onDestroy: () => void;
}

export const BubbleAssetFormExtension = (viewContainerRef: ViewContainerRef) => {
    let formFloating: FloatingUIInstance | undefined;
    let component: ComponentRef<AssetFormComponent>;
    let element: HTMLElement;
    let preventClose = false;
    let pendingPreventCloseReset: number | null = null;

    function onStart({ editor, type, getPosition }: StartProps) {
        setUpComponent(editor, type);

        formFloating = createFloatingUI(getPosition, element, {
            placement: 'bottom-start',
            offset: 8,
            zIndex: 10,
            onHide: () => onHide(editor),
            onClickOutside: () => onHide(editor)
        });
        formFloating.show();
    }

    function onHide(editor: Editor): void {
        if (preventClose) {
            return;
        }

        cancelPendingReset();
        if (!editor.isEditable) {
            editor.setOptions({ editable: true });
        }
        editor.commands.closeAssetForm();
        formFloating?.destroy();
        formFloating = undefined;
        component?.destroy();
    }

    function onDestroy() {
        cancelPendingReset();
        formFloating?.destroy();
        formFloating = undefined;
        component?.destroy();
    }

    function cancelPendingReset() {
        if (pendingPreventCloseReset !== null) {
            cancelAnimationFrame(pendingPreventCloseReset);
            pendingPreventCloseReset = null;
        }
    }

    function setUpComponent(editor: Editor, type: EditorAssetTypes) {
        component = viewContainerRef.createComponent(AssetFormComponent);
        component.instance.languageId = editor.storage.dotConfig.lang;
        component.instance.type = type;
        component.instance.onSelectAsset = (payload) => {
            onPreventClose(editor, false);
            editor.chain().insertAsset({ type, payload }).addNextLine().closeAssetForm().run();
        };

        component.instance.preventClose = (value) => onPreventClose(editor, value);

        component.instance.onHide = () => {
            onPreventClose(editor, false);
            onHide(editor);
        };

        element = component.location.nativeElement as HTMLElement;
        component.changeDetectorRef.detectChanges();
    }

    /**
     * Toggles preventClose flag and editor editable state.
     * When preventClose is true, the editor is set to non-editable to prevent
     * focus-driven hide while a dialog (e.g. file picker) is open.
     */
    function onPreventClose(editor: Editor, value: boolean) {
        cancelPendingReset();
        preventClose = value;
        editor.setOptions({ editable: !value });
    }

    return BubbleMenu.extend<unknown>({
        name: 'bubbleAssetForm',

        addOptions() {
            return {
                element: null,
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
                                preventClose = true;
                                tr.setMeta(BUBBLE_ASSET_FORM_PLUGIN_KEY, { open: true, type });
                                // Reset preventClose after the current frame so the focus
                                // handler (triggered by the blur/focus cycle of opening the
                                // form) does not immediately close it.
                                cancelPendingReset();
                                pendingPreventCloseReset = requestAnimationFrame(() => {
                                    preventClose = false;
                                    pendingPreventCloseReset = null;
                                });

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
                    ({ type, payload, position }) =>
                    ({ chain }) => {
                        switch (type) {
                            case 'video': {
                                if (typeof payload === 'string')
                                    return (
                                        // This method returns true if it was able to set the youtube video
                                        chain().setYoutubeVideo?.({ src: payload }).run() ||
                                        chain().insertVideo?.(payload, position).run()
                                    );
                                else return chain().insertVideo?.(payload, position).run();
                            }

                            case 'image':
                                return chain().insertImage(payload, position).run();
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
                        }) as RenderProps
                })
            ];
        }
    });
};
