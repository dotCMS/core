import { PluginKey } from 'prosemirror-state';
import { Subject } from 'rxjs';
import { Props } from 'tippy.js';

import { ViewContainerRef } from '@angular/core';

import { Extension } from '@tiptap/core';

import { AIContentPromptComponent } from './ai-content-prompt.component';
import { aiContentPromptPlugin } from './plugins/ai-content-prompt.plugin';

export const AI_CONTENT_PROMPT_PLUGIN_KEY = new PluginKey('aiContentPrompt-form');

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        AIContentPrompt: {
            openAIPrompt: () => ReturnType;
            closeAIPrompt: () => ReturnType;
        };
    }
}

export interface AIContentPromptOptions {
    pluginKey: PluginKey;
    tippyOptions?: Partial<Props>;
    element: HTMLElement | null;
}

export const AIContentPromptExtension = (viewContainerRef: ViewContainerRef) => {
    const formValue$ = new Subject<{ [key: string]: string }>();

    return Extension.create({
        name: 'aiContentPrompt',

        defaultOptions: {
            element: null,
            tippyOptions: {},
            pluginKey: AI_CONTENT_PROMPT_PLUGIN_KEY
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
                    ({ tr, dispatch }) => {
                        if (dispatch) {
                            tr.setMeta(this.options.pluginKey, { open: false });

                            return true;
                        }

                        return false;
                    },
                updateValue:
                    (formValue) =>
                    ({ editor }) => {
                        formValue$.next(formValue);
                        editor.commands.closeForm();
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
                    component: component,
                    form$: formValue$
                })
            ];
        }
    });
};
