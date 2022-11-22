import { ViewContainerRef, Injector } from '@angular/core';
import { PluginKey } from 'prosemirror-state';
import { Extension } from '@tiptap/core';

import { FloatingButtonComponent } from './floating-button.component';
import { DotFloatingButtonPlugin } from './plugin/floating-button.plugin';
import { DotImageService } from '../image-uploader/services/dot-image/dot-image.service';

export const FLOATING_BUTTON_PLUGIN_KEY = new PluginKey('floating-button');

export function DotFloatingButton(injector: Injector, viewContainerRef: ViewContainerRef) {
    // Create Instance Component
    const floatingButtonComponent = viewContainerRef.createComponent(FloatingButtonComponent);
    const floatingButtonElement = floatingButtonComponent.location.nativeElement;

    // Services
    const dotImageService = injector.get(DotImageService);

    return Extension.create({
        addProseMirrorPlugins() {
            if (!floatingButtonElement) {
                return [];
            }

            return [
                DotFloatingButtonPlugin({
                    ...this.options,
                    dotImageService,
                    component: floatingButtonComponent,
                    pluginKey: FLOATING_BUTTON_PLUGIN_KEY,
                    editor: this.editor,
                    element: floatingButtonElement
                })
            ];
        }
    });
}
