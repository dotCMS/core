import { Node } from 'prosemirror-model';
import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Subject } from 'rxjs';
import { Instance, Props } from 'tippy.js';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { takeUntil } from 'rxjs/operators';

import { Editor } from '@tiptap/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotGeneratedAIImage } from '@dotcms/dotcms-models';
import { DotAIImagePromptComponent } from '@dotcms/ui';

import { AI_IMAGE_PROMPT_PLUGIN_KEY } from './ai-image-prompt.extension';

interface AIImagePromptProps {
    pluginKey: PluginKey;
    editor: Editor;
    dialogService: DialogService;
    dotMessageService: DotMessageService;
}

interface PluginState {
    aIImagePromptOpen: boolean;
}

export type AIImagePromptViewProps = AIImagePromptProps & {
    view: EditorView;
};

export class AIImagePromptView {
    public editor: Editor;

    public node: Node;

    public view: EditorView;

    public tippy: Instance | undefined;

    public tippyOptions: Partial<Props>;

    public pluginKey: PluginKey;

    private destroy$ = new Subject<boolean>();

    #dialogService: DialogService | null = null;
    #dotMessageService: DotMessageService | null = null;
    #dialogRef: DynamicDialogRef | null = null;

    /**
     * Creates a new instance of the AIImagePromptView class.
     * @param {AIImagePromptViewProps} props - The properties for the component.
     */
    constructor(props: AIImagePromptViewProps) {
        const { editor, view, pluginKey, dialogService, dotMessageService } = props;

        this.editor = editor;
        this.view = view;

        this.pluginKey = pluginKey;
        this.#dialogService = dialogService;
        this.#dotMessageService = dotMessageService;
    }

    update(view: EditorView, prevState: EditorState) {
        const next = this.pluginKey?.getState(view.state);
        const prev = prevState ? this.pluginKey?.getState(prevState) : { aIImagePromptOpen: false };

        // show the dialog
        if (next.aIImagePromptOpen && prev.aIImagePromptOpen === false) {
            const context = this.editor.getText();

            const header = this.#dotMessageService.get(
                'block-editor.extension.ai-image.dialog-title'
            );

            this.#dialogRef = this.#dialogService.open(DotAIImagePromptComponent, {
                header,
                appendTo: 'body',
                closeOnEscape: false,
                draggable: false,
                keepInViewport: false,
                maskStyleClass: 'p-dialog-mask-transparent-ai',
                resizable: false,
                modal: true,
                width: '90%',
                style: { 'max-width': '1040px' },
                data: { context }
            });

            this.#dialogRef.onClose
                .pipe(takeUntil(this.destroy$))
                .subscribe((selectedImage: DotGeneratedAIImage) => {
                    if (selectedImage) {
                        this.editor.chain().insertImage(selectedImage.response.contentlet).run();
                    }

                    this.editor.commands.closeImagePrompt();
                });
        }
    }

    destroy() {
        this.destroy$.next(true);
        this.destroy$.complete();
        this.#dialogRef?.close();
    }
}

export const aiImagePromptPlugin = (options: AIImagePromptProps) => {
    return new Plugin({
        key: options.pluginKey as PluginKey,
        view: (view) => new AIImagePromptView({ view, ...options }),
        state: {
            init(): PluginState {
                return {
                    aIImagePromptOpen: false
                };
            },

            apply(
                transaction: Transaction,
                value: PluginState,
                oldState: EditorState
            ): PluginState {
                const { aIImagePromptOpen } = transaction.getMeta(AI_IMAGE_PROMPT_PLUGIN_KEY) || {};
                const state = AI_IMAGE_PROMPT_PLUGIN_KEY.getState(oldState);
                if (typeof aIImagePromptOpen === 'boolean') {
                    return { aIImagePromptOpen };
                }

                return state || value;
            }
        }
    });
};
