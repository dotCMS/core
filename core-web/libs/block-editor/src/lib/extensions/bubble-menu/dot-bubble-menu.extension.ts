import { PluginKey } from 'prosemirror-state';
import { Props } from 'tippy.js';

import { ViewContainerRef } from '@angular/core';

import BubbleMenu, { BubbleMenuOptions } from '@tiptap/extension-bubble-menu';

import { BubbleMenuComponent } from './bubble-menu.component';
import { DotBubbleMenuPlugin } from './plugins/dot-bubble-menu.plugin';
import { shouldShowBubbleMenu } from './utils/index';

import { SuggestionsComponent } from '../../shared';

const defaultTippyOptions: Partial<Props> = {
    duration: 500,
    maxWidth: 'none',
    placement: 'top-start',
    trigger: 'manual',
    interactive: true
};

export const BUBBLE_MENU_PLUGIN_KEY = new PluginKey('bubble-menu');

export function DotBubbleMenuExtension(viewContainerRef: ViewContainerRef) {
    // Create Instance Component
    const bubbleMenuComponent = viewContainerRef.createComponent(BubbleMenuComponent);
    const bubbleMenuElement = bubbleMenuComponent.location.nativeElement;

    // Create ChangeTo Component Instance
    const changeToComponent = viewContainerRef.createComponent(SuggestionsComponent);
    const changeToElement = changeToComponent.location.nativeElement;

    return BubbleMenu.extend<BubbleMenuOptions>({
        name: 'bubbleMenu',
        // Default Options
        addOptions(): BubbleMenuOptions {
            return {
                element: null,
                tippyOptions: defaultTippyOptions,
                pluginKey: 'bubbleMenu',
                shouldShow: shouldShowBubbleMenu
            };
        },

        addStorage() {
            return {
                changeToIsOpen: false
            };
        },

        addProseMirrorPlugins() {
            if (!bubbleMenuElement) {
                return [];
            }

            return [
                DotBubbleMenuPlugin({
                    ...this.options,
                    component: bubbleMenuComponent,
                    changeToComponent: changeToComponent,
                    pluginKey: BUBBLE_MENU_PLUGIN_KEY,
                    editor: this.editor,
                    element: bubbleMenuElement,
                    changeToElement: changeToElement
                })
            ];
        }
    });
}
