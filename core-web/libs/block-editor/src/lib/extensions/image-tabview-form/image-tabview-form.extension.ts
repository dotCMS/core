import { PluginKey } from 'prosemirror-state';
import { Props } from 'tippy.js';

import { ViewContainerRef } from '@angular/core';

import BubbleMenu from '@tiptap/extension-bubble-menu';

import { ImageTabviewFormComponent } from './image-tabview-form.component';
import { bubbleImageTabviewFormPlugin } from './plugins/bubble-image-tabview-form.plugin';

export const BUBBLE_IMAGE_TABVIEW_FORM_PLUGIN_KEY = new PluginKey('bubble-image-form');

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

export const BubbleImageTabviewFormExtension = (viewContainerRef: ViewContainerRef) => {
    return BubbleMenu.extend<unknown>({
        name: 'bubbleImageForm',

        addOptions() {
            return {
                element: null,
                tippyOptions: {},
                pluginKey: BUBBLE_IMAGE_TABVIEW_FORM_PLUGIN_KEY
            };
        },

        addProseMirrorPlugins() {
            const component = viewContainerRef.createComponent(ImageTabviewFormComponent);
            const componentElement = component.location.nativeElement;
            component.changeDetectorRef.detectChanges();

            return [
                bubbleImageTabviewFormPlugin({
                    pluginKey: BUBBLE_IMAGE_TABVIEW_FORM_PLUGIN_KEY,
                    editor: this.editor,
                    element: componentElement,
                    tippyOptions: tippyOptions,
                    component: component
                })
            ];
        }
    });
};
