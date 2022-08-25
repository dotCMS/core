import { Extension } from '@tiptap/core';
import { PluginKey } from 'prosemirror-state';
import { ViewContainerRef } from '@angular/core';
import { Props } from 'tippy.js';

import { bubbleLinkFormPlugin } from './plugins/bubble-link-form.plugin';
import { BubbleLinkFormComponent } from './bubble-link-form.component';

export interface BubbleLinkFormOptions {
    pluginKey: PluginKey;
    tippyOptions?: Partial<Props>;
    element: HTMLElement | null;
}

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        bubbleLinkForm: {
            openLinkForm: ({ openOnClick }) => ReturnType;
            closeLinkForm: () => ReturnType;
        };
    }
}

export const LINK_FORM_PLUGIN_KEY = new PluginKey('addLink');

export const BubbleLinkFormExtension = (viewContainerRef: ViewContainerRef) => {
    return Extension.create<BubbleLinkFormOptions>({
        name: 'bubbleLinkForm',

        addOptions() {
            return {
                element: null,
                tippyOptions: {},
                pluginKey: LINK_FORM_PLUGIN_KEY
            };
        },

        addCommands() {
            return {
                openLinkForm:
                    ({ openOnClick }) =>
                    ({ chain }) => {
                        return chain()
                            .setHighlight()
                            .command(({ tr }) => {
                                tr.setMeta(LINK_FORM_PLUGIN_KEY, { isOpen: true, openOnClick });

                                return true;
                            })
                            .run();
                    },
                closeLinkForm:
                    () =>
                    ({ chain }) => {
                        return chain()
                            .unsetHighlight()
                            .command(({ tr }) => {
                                tr.setMeta(LINK_FORM_PLUGIN_KEY, {
                                    isOpen: false,
                                    openOnClick: false
                                });

                                return true;
                            })
                            .run();
                    }
            };
        },

        addProseMirrorPlugins() {
            const component = viewContainerRef.createComponent(BubbleLinkFormComponent);
            component.changeDetectorRef.detectChanges();

            return [
                bubbleLinkFormPlugin({
                    pluginKey: this.options.pluginKey,
                    editor: this.editor,
                    element: component.location.nativeElement,
                    tippyOptions: this.options.tippyOptions,
                    component: component
                })
            ];
        }
    });
};
