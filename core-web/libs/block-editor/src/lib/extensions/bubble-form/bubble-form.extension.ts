import BubbleMenu from '@tiptap/extension-bubble-menu';
import { PluginKey } from 'prosemirror-state';
import { ViewContainerRef } from '@angular/core';

import { bubbleFormPlugin } from './plugins/bubble-form.plugin';
import { BubbleFormComponent } from './bubble-form.component';
import { Props } from 'tippy.js';
import { DynamicControl } from './model';
import { Subject } from 'rxjs';

export const BUBBLE_FORM_PLUGIN_KEY = new PluginKey('bubble-form');

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        BubbleForm: {
            openForm: (
                form?: DynamicControl<string | boolean>[],
                options?: { customClass: string }
                // eslint-disable-next-line
            ) => any;
            closeForm: () => ReturnType;
            updateValue: (value) => void;
        };
    }
}

const tippyOptions: Partial<Props> = {
    interactive: true,
    maxWidth: 'none',
    trigger: 'manual',
    placement: 'bottom-start',
    hideOnClick: 'toggle',
    popperOptions: {
        modifiers: [
            {
                name: 'flip',
                options: { fallbackPlacements: ['top-start'] }
            }
        ]
    }
};

export const BubbleFormExtension = (viewContainerRef: ViewContainerRef) => {
    const formValue$ = new Subject<{ [key: string]: string }>();

    return BubbleMenu.extend<unknown>({
        name: 'bubbleForm',

        addOptions() {
            return {
                element: null,
                tippyOptions: {},
                pluginKey: BUBBLE_FORM_PLUGIN_KEY,
                shouldShow: () => true
            };
        },

        addCommands() {
            return {
                openForm:
                    (form, options) =>
                    ({ chain }) => {
                        chain()
                            .command(({ tr }) => {
                                tr.setMeta(BUBBLE_FORM_PLUGIN_KEY, { form, options, open: true });

                                return true;
                            })
                            .run();

                        return formValue$;
                    },
                closeForm:
                    () =>
                    ({ chain }) => {
                        formValue$.next(null);

                        return chain()
                            .command(({ tr }) => {
                                tr.setMeta(BUBBLE_FORM_PLUGIN_KEY, { open: false });

                                return true;
                            })
                            .run();
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
            const component = viewContainerRef.createComponent(BubbleFormComponent);
            const componentElement = component.location.nativeElement;
            component.changeDetectorRef.detectChanges();

            return [
                bubbleFormPlugin({
                    pluginKey: BUBBLE_FORM_PLUGIN_KEY,
                    editor: this.editor,
                    element: componentElement,
                    tippyOptions: tippyOptions,
                    component: component,
                    form$: formValue$
                })
            ];
        }
    });
};
