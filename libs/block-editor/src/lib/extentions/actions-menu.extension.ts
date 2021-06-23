import { ComponentFactoryResolver, Injector } from '@angular/core';

import { Extension, Range } from '@tiptap/core';
import { FloatingMenuPluginProps } from '@tiptap/extension-floating-menu';

import tippy from 'tippy.js';

import { FloatingActionsPlugin } from '../plugins/floating.plugin';
import { SuggestionsComponent } from '../suggestions/suggestions.component';
import { ActionButtonComponent } from './action-button/action-button.component';

export type FloatingMenuOptions = Omit<FloatingMenuPluginProps, 'editor' | 'element'> & {
    element: HTMLElement | null;
};

export const ActionsMenu = (injector: Injector, resolver: ComponentFactoryResolver) => {
    return Extension.create<FloatingMenuOptions>({
        name: 'actionsMenu',

        defaultOptions: {
            element: null,
            tippyOptions: {}
        },

        addProseMirrorPlugins() {
            const factoryButton = resolver.resolveComponentFactory(ActionButtonComponent);
            const button = factoryButton.create(injector);

            let myTippy;

            return [
                FloatingActionsPlugin({
                    editor: this.editor,
                    element: button.location.nativeElement,
                    onAction: (rect: DOMRect, range: Range) => {
                        const factorySuggestions = resolver.resolveComponentFactory(
                            SuggestionsComponent
                        );
                        const suggestions = factorySuggestions.create(injector);

                        suggestions.instance.command = (item) => {
                            this.editor
                                .chain()
                                .focus()
                                .insertContentAt(range, {
                                    type: 'dotContent',
                                    attrs: {
                                        data: item
                                    }
                                })
                                .run();
                            myTippy.destroy();
                        };
                        suggestions.changeDetectorRef.detectChanges();

                        myTippy = tippy(this.editor.view.dom, {
                            appendTo: document.body,
                            content: suggestions.location.nativeElement,
                            placement: 'auto-start',
                            getReferenceClientRect: () => rect,
                            showOnCreate: true,
                            interactive: true,
                            trigger: 'manual'
                        });
                    }
                })
            ];
        }
    });
};
