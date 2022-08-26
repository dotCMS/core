import BubbleMenu from '@tiptap/extension-bubble-menu';
import { PluginKey } from 'prosemirror-state';
import { ViewContainerRef } from '@angular/core';

import { bubbleFormPlugin } from './plugins/bubble-form.plugin';
import { BubbleFormComponent } from './bubble-form.component';

export const BUBBLE_FORM_PLUGIN_KEY = new PluginKey('bubble-form');

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        BubbleForm: {
            openForm: () => ReturnType;
            closeForm: () => ReturnType;
        };
    }
}

const tippyOptions = {
    interactive: true,
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

export const BubbleFormExtension = (viewContainerRef: ViewContainerRef) => {
    return BubbleMenu.extend<unknown>({
        name: 'bubbleForm',

        addOptions() {
            return {
                element: null,
                tippyOptions: {},
                pluginKey: BUBBLE_FORM_PLUGIN_KEY,
                shouldShow: () => true
            };
        },

        addCommands() {
            return {
                openForm:
                    () =>
                    ({ chain }) => {
                        return chain()
                            .command(({ tr }) => {
                                tr.setMeta(BUBBLE_FORM_PLUGIN_KEY, { open: true });

                                return true;
                            })
                            .run();
                    },
                closeForm:
                    () =>
                    ({ chain }) => {
                        return chain()
                            .command(({ tr }) => {
                                tr.setMeta(BUBBLE_FORM_PLUGIN_KEY, { open: false });

                                return true;
                            })
                            .run();
                    }
            };
        },

        addProseMirrorPlugins() {
            const component = viewContainerRef.createComponent(BubbleFormComponent);
            const componentElement = component.location.nativeElement;
            component.changeDetectorRef.detectChanges();

            return [
                bubbleFormPlugin({
                    pluginKey: BUBBLE_FORM_PLUGIN_KEY,
                    editor: this.editor,
                    element: componentElement,
                    tippyOptions: tippyOptions,
                    component: component
                })
            ];
        }
    });
};
