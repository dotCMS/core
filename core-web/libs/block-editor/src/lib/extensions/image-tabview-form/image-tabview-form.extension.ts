import BubbleMenu from '@tiptap/extension-bubble-menu';
import { PluginKey } from 'prosemirror-state';
import { ViewContainerRef, ComponentRef } from '@angular/core';

import { ImageTabviewFormComponent } from './image-tabview-form.component';
import { bubbleImageTabviewFormPlugin } from './plugins/bubble-image-tabview-form.plugin';

import { Editor } from '@tiptap/core';
import tippy, { Instance, Props, GetReferenceClientRect } from 'tippy.js';

export const BUBBLE_IMAGE_TABVIEW_FORM_PLUGIN_KEY = new PluginKey('bubble-image-form');

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        ImageTabviewForm: {
            toggleImageForm: (value: boolean) => ReturnType;
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

interface onStartProps {
    editor: Editor;
    getPosition: GetReferenceClientRect;
}

export interface RenderProps {
    onStart: (value: onStartProps) => void;
    onHide: (editor: Editor) => void;
    onDestroy: () => void;
}

export const BubbleImageTabviewFormExtension = (viewContainerRef: ViewContainerRef) => {
    let formTippy: Instance | undefined;
    let component: ComponentRef<ImageTabviewFormComponent>;
    let element: Element;

    function onStart({ editor, getPosition }: onStartProps) {
        setUpTippy(editor);
        setUpComponent(editor);

        formTippy.setProps({
            content: element,
            getReferenceClientRect: getPosition,
            onClickOutside: () => onHide(editor)
        });
        formTippy.show();
    }

    function onHide(editor): void {
        editor.commands.toggleImageForm(false);
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

    function setUpComponent(editor: Editor) {
        component = viewContainerRef.createComponent(ImageTabviewFormComponent);
        component.instance.languageId = editor.storage.dotConfig.lang;
        component.instance.onSelectImage = (payload) => {
            editor.chain().addDotImage(payload).addNextLine().toggleImageForm(false).run();
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
                toggleImageForm:
                    (value) =>
                    ({ chain }) => {
                        return chain()
                            .command(({ tr }) => {
                                tr.setMeta(BUBBLE_IMAGE_TABVIEW_FORM_PLUGIN_KEY, {
                                    open: value
                                });

                                return true;
                            })
                            .run();
                    }
            };
        },

        addProseMirrorPlugins() {
            return [
                bubbleImageTabviewFormPlugin({
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
