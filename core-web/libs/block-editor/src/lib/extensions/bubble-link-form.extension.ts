import { Extension } from '@tiptap/core';
import { PluginKey } from 'prosemirror-state';
import { BubbleMenuLinkFormComponent } from './components/bubble-menu-link-form/bubble-menu-link-form.component';
import { ViewContainerRef } from '@angular/core';
import { bubbleLinkFormPlugin } from '../plugins/bubble-link-form.plugin';
import { Props } from 'tippy.js';

export interface BubbleLinkFormOptions {
    pluginKey: PluginKey;
    tippyOptions?: Partial<Props>;
    element: HTMLElement | null;
}

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        bubbleLinkForm: {
            openLinkForm: ({ fromClick }) => ReturnType;
            closeLinkForm: () => ReturnType;
        };
    }
}

export const LINK_FORM_PLUGIN_KEY = new PluginKey('addLink');

export const BubbleLinkFormExtension = (viewContainerRef: ViewContainerRef) => {
    return Extension.create<BubbleLinkFormOptions>({
        name: 'bubbleLinkForm',
        defaultOptions: {
            element: null,
            tippyOptions: {},
            pluginKey: LINK_FORM_PLUGIN_KEY
        },

        addCommands() {
            return {
                openLinkForm:
                    ({ fromClick }) =>
                    ({ chain }) => {
                        return chain()
                            .setHighlight()
                            .command(({ tr }) => {
                                tr.setMeta(LINK_FORM_PLUGIN_KEY, { isOpen: true, fromClick });
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
                                tr.setMeta(LINK_FORM_PLUGIN_KEY, { isOpen: false, fromClick: false });
                                return true;
                            })
                            .run();
                    }
            };
        },

        addProseMirrorPlugins() {
            const component = viewContainerRef.createComponent(BubbleMenuLinkFormComponent);
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
