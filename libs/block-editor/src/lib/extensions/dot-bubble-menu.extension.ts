import { ViewContainerRef, Type } from '@angular/core';
import BubbleMenu, { BubbleMenuOptions } from '@tiptap/extension-bubble-menu';
import { Props } from 'tippy.js';
import { DotBubbleMenuPlugin } from '../plugins/dot-bubble-menu.plugin';
import { shouldShowBubbleMenu } from '../utils/bubble-menu.utils';
import { BubbleMenuComponentProps } from '@dotcms/block-editor';

const defaultTippyOptions: Partial<Props> = {
    appendTo: document.body,
    duration: 500,
    maxWidth: 'none',
    placement: 'top-start',
    trigger: 'manual'
};

export function DotBubbleMenuExtension(
    type: Type<BubbleMenuComponentProps>,
    viewContainerRef: ViewContainerRef
) {
    // Create Instance Component
    const angularComponent = viewContainerRef.createComponent(type);
    const htmlElement = angularComponent.location.nativeElement;

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
            if (!htmlElement) {
                return [];
            }

            return [
                DotBubbleMenuPlugin({
                    component: angularComponent,
                    pluginKey: this.options.pluginKey,
                    editor: this.editor,
                    element: htmlElement,
                    tippyOptions: this.options.tippyOptions,
                    shouldShow: this.options.shouldShow
                })
            ];
        }
    });
}
