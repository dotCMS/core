import { ViewContainerRef } from '@angular/core';
import BubbleMenu, { BubbleMenuOptions } from '@tiptap/extension-bubble-menu';
import { Props } from 'tippy.js';
import { DotBubbleMenuPlugin } from '../plugins/dot-bubble-menu.plugin';
import { shouldShowBubbleMenu } from '../utils/bubble-menu.utils';
import { BubbleMenuComponent, SuggestionsComponent } from '@dotcms/block-editor';

const defaultTippyOptions: Partial<Props> = {
    duration: 500,
    maxWidth: 'none',
    placement: 'top-start',
    trigger: 'manual',
    interactive: true
};

export function DotBubbleMenuExtension(viewContainerRef: ViewContainerRef) {
    // Create Instance Component
    const bubbleMenuComponent = viewContainerRef.createComponent(BubbleMenuComponent);
    const bubbleMenuElement = bubbleMenuComponent.location.nativeElement;

    // Create ChangeTo Component Instance
    const changeToComponent = viewContainerRef.createComponent(SuggestionsComponent);
    const changeToElement = changeToComponent.location.nativeElement;

    return BubbleMenu.extend<BubbleMenuOptions>({
        // Default Options
        addOptions(): BubbleMenuOptions {
            return {
                element: null,
                tippyOptions: defaultTippyOptions,
                pluginKey: 'bubbleMenu',
                shouldShow: shouldShowBubbleMenu
            };
        },

        addProseMirrorPlugins() {
            if (!bubbleMenuElement) {
                return [];
            }

            return [
                DotBubbleMenuPlugin({
                    component: bubbleMenuComponent,
                    changeToComponent: changeToComponent,
                    pluginKey: this.options.pluginKey,
                    editor: this.editor,
                    element: bubbleMenuElement,
                    changeToElement: changeToElement,
                    tippyOptions: this.options.tippyOptions,
                    shouldShow: this.options.shouldShow
                })
            ];
        }
    });
}
