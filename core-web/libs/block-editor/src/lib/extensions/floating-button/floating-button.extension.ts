import { ViewContainerRef } from '@angular/core';
import { PluginKey } from 'prosemirror-state';
import { Extension } from '@tiptap/core';

import { FloatingButtonComponent } from './floating-button.component';
import { DotFloatingButtonPlugin } from './plugin/floating-button.plugin';

export const FLOATING_BUTTON_PLUGIN_KEY = new PluginKey('floating-button');

export function DotFloatingButton(viewContainerRef: ViewContainerRef) {
    // Create Instance Component
    const floatingButtonComponent = viewContainerRef.createComponent(FloatingButtonComponent);
    const floatingButtonElement = floatingButtonComponent.location.nativeElement;

    return Extension.create({
        addProseMirrorPlugins() {
            if (!floatingButtonElement) {
                return [];
            }

            return [
                DotFloatingButtonPlugin({
                    ...this.options,
                    component: floatingButtonComponent,
                    pluginKey: FLOATING_BUTTON_PLUGIN_KEY,
                    editor: this.editor,
                    element: floatingButtonElement
                })
            ];
        }
    });
}
