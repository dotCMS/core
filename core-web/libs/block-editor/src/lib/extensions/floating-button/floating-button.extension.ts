import { PluginKey } from 'prosemirror-state';

import { Injector, ViewContainerRef } from '@angular/core';

import { Extension } from '@tiptap/core';

import { DotUploadFileService } from '@dotcms/data-access';

import { FloatingButtonComponent } from './floating-button.component';
import { DotFloatingButtonPlugin } from './plugin/floating-button.plugin';

export const FLOATING_BUTTON_PLUGIN_KEY = new PluginKey('floating-button');

export function DotFloatingButton(injector: Injector, viewContainerRef: ViewContainerRef) {
    // Create Instance Component
    const floatingButtonComponent = viewContainerRef.createComponent(FloatingButtonComponent);
    const floatingButtonElement = floatingButtonComponent.location.nativeElement;

    // Services
    const dotUploadFileService = injector.get(DotUploadFileService);

    return Extension.create({
        addProseMirrorPlugins() {
            if (!floatingButtonElement) {
                return [];
            }

            return [
                DotFloatingButtonPlugin({
                    ...this.options,
                    dotUploadFileService,
                    component: floatingButtonComponent,
                    pluginKey: FLOATING_BUTTON_PLUGIN_KEY,
                    editor: this.editor,
                    element: floatingButtonElement
                })
            ];
        }
    });
}
