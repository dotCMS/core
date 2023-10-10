import { PluginKey } from 'prosemirror-state';
import { Props } from 'tippy.js';

import { ViewContainerRef } from '@angular/core';

import { Extension } from '@tiptap/core';

import { AIContentActionsComponent } from './ai-content-actions.component';
import { aiContentActionsPlugin } from './plugins/ai-content-actions.plugin';

export interface AIContentActionsOptions {
    pluginKey: PluginKey;
    tippyOptions?: Partial<Props>;
    element: HTMLElement | null;
}

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        AIContentActions: {
            openAIContentActions: () => ReturnType;
            closeAIContentActions: () => ReturnType;
        };
    }
}

export const AI_CONTENT_ACTIONS_PLUGIN_KEY = new PluginKey('aiContentActions-form');

export const AIContentActionsExtension = (viewContainerRef: ViewContainerRef) => {
    return Extension.create<AIContentActionsOptions>({
        name: 'aiContentActions',

        addOptions() {
            return {
                element: null,
                tippyOptions: {},
                pluginKey: AI_CONTENT_ACTIONS_PLUGIN_KEY
            };
        },

        addCommands() {
            return {
                openAIContentActions:
                    () =>
                    ({ chain }) => {
                        return chain()
                            .command(({ tr }) => {
                                tr.setMeta(AI_CONTENT_ACTIONS_PLUGIN_KEY, { open: true });

                                return true;
                            })
                            .freezeScroll(true)
                            .run();
                    },
                closeAIContentActions:
                    () =>
                    ({ chain }) => {
                        return chain()
                            .command(({ tr }) => {
                                tr.setMeta(AI_CONTENT_ACTIONS_PLUGIN_KEY, { open: false });

                                return true;
                            })
                            .freezeScroll(false)
                            .run();
                    }
            };
        },

        addProseMirrorPlugins() {
            const component = viewContainerRef.createComponent(AIContentActionsComponent);
            component.changeDetectorRef.detectChanges();

            return [
                aiContentActionsPlugin({
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
