import { PluginKey } from 'prosemirror-state';
import { Subject } from 'rxjs';
import { Props } from 'tippy.js';

import { ViewContainerRef } from '@angular/core';

import { CommandProps } from '@tiptap/core';
import BubbleMenu from '@tiptap/extension-bubble-menu';

import { BubbleFormComponent } from './bubble-form.component';
import { BubbleFormValue, DynamicControl } from './model';
import { bubbleFormPlugin } from './plugins/bubble-form.plugin';

export const BUBBLE_FORM_PLUGIN_KEY = new PluginKey('bubble-form');

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        BubbleForm: {
            /**
             * Opens the bubble form. Returns the `Subject<BubbleFormValue>` that emits the
             * result — which is not a `ReturnType`, so this command cannot be chained.
             *
             * Declared `any` on purpose: TipTap constrains every entry in `addCommands()` to
             * `(...args) => Command`, and a command returning a Subject does not satisfy it.
             * Callers should narrow with `Observable<BubbleFormValue>` at the call site.
             */
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            openForm: (
                form?: DynamicControl<string | boolean>[],
                options?: { customClass: string }
            ) => any;
            closeForm: () => ReturnType;
            updateValue: (value: BubbleFormValue) => void;
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
                name: 'animate-flip',
                options: { fallbackPlacements: ['top-start'] }
            }
        ]
    }
};

export const BubbleFormExtension = (viewContainerRef: ViewContainerRef) => {
    const formValue$ = new Subject<BubbleFormValue>();

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
                // `chain` is annotated because `openForm` is declared `any` above, which stops
                // TipTap from inferring `CommandProps` for this entry.
                openForm:
                    (form, options) =>
                    ({ chain }: CommandProps) => {
                        chain()
                            .command(({ tr }) => {
                                tr.setMeta(BUBBLE_FORM_PLUGIN_KEY, { form, options, open: true });

                                return true;
                            })
                            .freezeScroll(true)
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
                            .freezeScroll(false)
                            .run();
                    },
                updateValue:
                    (formValue) =>
                    ({ editor }: CommandProps) => {
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
