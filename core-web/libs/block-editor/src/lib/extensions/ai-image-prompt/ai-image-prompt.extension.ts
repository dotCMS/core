import { PluginKey } from 'prosemirror-state';

import { ViewContainerRef } from '@angular/core';

import { Extension } from '@tiptap/core';

import { DotAIImagePromptComponent } from '@dotcms/ui';

import { aiImagePromptPlugin } from './ai-image-prompt.plugin';

export interface AIImagePromptOptions {
    pluginKey: PluginKey;
    element: HTMLElement | null;
}

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        AIImagePrompt: {
            openImagePrompt: () => ReturnType;
            closeImagePrompt: () => ReturnType;
        };
    }
}

export const DOT_AI_IMAGE_CONTENT_KEY = 'dotAIImageContent';

export const AI_IMAGE_PROMPT_PLUGIN_KEY = new PluginKey('aiImagePrompt-form');

export const AI_IMAGE_PROMPT_EXTENSION_NAME = 'aiImagePrompt';

export const AIImagePromptExtension = (viewContainerRef: ViewContainerRef) => {
    return Extension.create<AIImagePromptOptions>({
        name: AI_IMAGE_PROMPT_EXTENSION_NAME,

        addOptions() {
            return {
                element: null,
                pluginKey: AI_IMAGE_PROMPT_PLUGIN_KEY
            };
        },

        addCommands() {
            return {
                openImagePrompt:
                    () =>
                    ({ chain }) => {
                        return chain()
                            .command(({ tr }) => {
                                tr.setMeta(AI_IMAGE_PROMPT_PLUGIN_KEY, { aIImagePromptOpen: true });

                                return true;
                            })
                            .freezeScroll(true)
                            .run();
                    },
                closeImagePrompt:
                    () =>
                    ({ chain }) => {
                        return chain()
                            .command(({ tr }) => {
                                tr.setMeta(AI_IMAGE_PROMPT_PLUGIN_KEY, {
                                    aIImagePromptOpen: false
                                });

                                return true;
                            })
                            .freezeScroll(false)
                            .run();
                    }
            };
        },

        addProseMirrorPlugins() {
            const component = viewContainerRef.createComponent(DotAIImagePromptComponent);
            component.changeDetectorRef.detectChanges();

            return [
                aiImagePromptPlugin({
                    pluginKey: this.options.pluginKey,
                    editor: this.editor,
                    element: component.location.nativeElement,
                    component: component
                })
            ];
        }
    });
};
