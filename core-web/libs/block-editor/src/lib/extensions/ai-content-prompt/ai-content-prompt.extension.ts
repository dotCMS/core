import { PluginKey } from 'prosemirror-state';

import { ViewContainerRef } from '@angular/core';

import { Extension } from '@tiptap/core';

import { AIContentPromptComponent } from './ai-content-prompt.component';
import { aiContentPromptPlugin } from './plugins/ai-content-prompt.plugin';
// Import to ensure type augmentation is available
import '../freeze-scroll/freeze-scroll.extension';

export interface AIContentPromptOptions {
    pluginKey: PluginKey;
    element: HTMLElement | null;
}

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        AIContentPrompt: {
            openAIPrompt: () => ReturnType;
            closeAIPrompt: () => ReturnType;
        };
    }
}

export const DOT_AI_TEXT_CONTENT_KEY = 'dotAITextContent';

export const AI_CONTENT_PROMPT_PLUGIN_KEY = new PluginKey(DOT_AI_TEXT_CONTENT_KEY);

export const AI_CONTENT_PROMPT_EXTENSION_NAME = 'aiContentPrompt';

export const AIContentPromptExtension = (viewContainerRef: ViewContainerRef) => {
    return Extension.create<AIContentPromptOptions>({
        name: AI_CONTENT_PROMPT_EXTENSION_NAME,

        addOptions() {
            return {
                element: null,
                pluginKey: AI_CONTENT_PROMPT_PLUGIN_KEY
            };
        },

        addCommands() {
            return {
                openAIPrompt:
                    () =>
                    ({ chain }) => {
                        return chain()
                            .command(({ tr }) => {
                                tr.setMeta(AI_CONTENT_PROMPT_PLUGIN_KEY, {
                                    aIContentPromptOpen: true
                                });

                                return true;
                            })
                            .freezeScroll(true)
                            .run();
                    },
                closeAIPrompt:
                    () =>
                    ({ chain }) => {
                        return chain()
                            .command(({ tr }) => {
                                tr.setMeta(AI_CONTENT_PROMPT_PLUGIN_KEY, {
                                    aIContentPromptOpen: false
                                });

                                return true;
                            })
                            .freezeScroll(false)
                            .run();
                    }
            };
        },

        addProseMirrorPlugins() {
            const component = viewContainerRef.createComponent(AIContentPromptComponent);
            component.changeDetectorRef.detectChanges();

            return [
                aiContentPromptPlugin({
                    pluginKey: this.options.pluginKey,
                    editor: this.editor,
                    element: component.location.nativeElement,
                    component: component
                })
            ];
        }
    });
};
