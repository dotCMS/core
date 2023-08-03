import { PluginKey } from 'prosemirror-state';
import { Props } from 'tippy.js';

import { ViewContainerRef } from '@angular/core';

import { Extension } from '@tiptap/core';

import { AIContentPromptComponent } from './ai-content-prompt.component';
import { aiContentPromptPlugin } from './plugins/ai-content-prompt.plugin';

export interface AIContentPromptOptions {
    pluginKey: PluginKey;
    tippyOptions?: Partial<Props>;
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

export const AI_CONTENT_PROMPT_PLUGIN_KEY = new PluginKey('aiContentPrompt-form');

export const AIContentPromptExtension = (viewContainerRef: ViewContainerRef) => {
    return Extension.create<AIContentPromptOptions>({
        name: 'aiContentPrompt',

        addOptions() {
            return {
                element: null,
                tippyOptions: {},
                pluginKey: AI_CONTENT_PROMPT_PLUGIN_KEY
            };
        },

        addCommands() {
            return {
                openAIPrompt:
                    () =>
                    ({ chain }) => {
                        chain()
                            .command(({ tr }) => {
                                tr.setMeta(AI_CONTENT_PROMPT_PLUGIN_KEY, { open: true });

                                return true;
                            })
                            .freezeScroll(true)
                            .run();

                        return true;
                    },
                closeAIPrompt:
                    () =>
                    ({ chain }) => {
                        return chain()
                            .command(({ tr }) => {
                                tr.setMeta(AI_CONTENT_PROMPT_PLUGIN_KEY, { open: false });

                                return true;
                            })
                            .freezeScroll(false)
                            .run();
                    },
                updateValue:
                    () =>
                    ({ editor }) => {
                        editor.commands.closeAIPrompt();
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
                    tippyOptions: this.options.tippyOptions,
                    component: component
                })
            ];
        }
    });
};
