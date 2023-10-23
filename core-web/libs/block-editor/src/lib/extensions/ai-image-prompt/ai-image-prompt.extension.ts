import { PluginKey } from 'prosemirror-state';
import { Props } from 'tippy.js';

import { ViewContainerRef } from '@angular/core';

import { Extension } from '@tiptap/core';

import { AIImagePromptComponent } from './ai-image-prompt.component';
import { aiImagePromptPlugin } from './plugins/ai-image-prompt.plugin';

export interface AIImagePromptOptions {
    pluginKey: PluginKey;
    tippyOptions?: Partial<Props>;
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

export const AI_IMAGE_PROMPT_PLUGIN_KEY = new PluginKey('aiImagePrompt-form');

export const AIImagePromptExtension = (viewContainerRef: ViewContainerRef) => {
    return Extension.create<AIImagePromptOptions>({
        name: 'aiImagePrompt',

        addOptions() {
            return {
                element: null,
                tippyOptions: {},
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
                                tr.setMeta(AI_IMAGE_PROMPT_PLUGIN_KEY, { open: true });

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
                                tr.setMeta(AI_IMAGE_PROMPT_PLUGIN_KEY, { open: false });

                                return true;
                            })
                            .freezeScroll(false)
                            .run();
                    }
            };
        },

        addProseMirrorPlugins() {
            const component = viewContainerRef.createComponent(AIImagePromptComponent);
            component.changeDetectorRef.detectChanges();

            return [
                aiImagePromptPlugin({
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
