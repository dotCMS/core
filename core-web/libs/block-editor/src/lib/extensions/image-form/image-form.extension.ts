import BubbleMenu from '@tiptap/extension-bubble-menu';
import { PluginKey } from 'prosemirror-state';
import { ViewContainerRef } from '@angular/core';

import { Props } from 'tippy.js';
import { ImageFormComponent } from './image-form.component';
import { bubbleImageFormPlugin } from './plugins/bubble-image-form';

export const BUBBLE_IMAGE_FORM_PLUGIN_KEY = new PluginKey('bubble-image-form');

const tippyOptions: Partial<Props> = {
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

export const BubbleImageFormExtension = (viewContainerRef: ViewContainerRef) => {
    return BubbleMenu.extend<unknown>({
        name: 'bubbleImageForm',

        addOptions() {
            return {
                element: null,
                tippyOptions: {},
                pluginKey: BUBBLE_IMAGE_FORM_PLUGIN_KEY
            };
        },

        addProseMirrorPlugins() {
            const component = viewContainerRef.createComponent(ImageFormComponent);
            const componentElement = component.location.nativeElement;
            component.changeDetectorRef.detectChanges();

            return [
                bubbleImageFormPlugin({
                    pluginKey: BUBBLE_IMAGE_FORM_PLUGIN_KEY,
                    editor: this.editor,
                    element: componentElement,
                    tippyOptions: tippyOptions,
                    component: component
                })
            ];
        }
    });
};
