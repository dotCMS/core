import { Injectable, NgZone, OnDestroy, inject, signal } from '@angular/core';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { Editor } from '@tiptap/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotGeneratedAIImage } from '@dotcms/dotcms-models';
import { DotAIImagePromptComponent, DotBrowserSelectorComponent } from '@dotcms/ui';

import { AiContentDialogComponent } from '../components/ai-content-dialog/ai-content-dialog.component';
import { OVERLAY_ABOVE_FULLSCREEN_Z_INDEX, buildBrowserSelectorConfig } from '../config.utils';
import {
    insertDotAudioFromContentlet,
    insertDotImageFromContentlet,
    insertDotVideoFromContentlet
} from '../editor.utils';

/**
 * Owns every centered modal dialog in the editor — all opened via PrimeNG's
 * {@link DialogService.open}: AI content, AI image, and the image / video / audio pickers.
 * Sibling to {@link EditorPopoverService}, which owns caret-anchored popovers
 * (table, link, emoji, image-properties).
 *
 * Provided at the editor component scope so each editor instance has its own modal refs
 * and per-instance teardown via {@link ngOnDestroy}.
 */
@Injectable()
export class EditorModalService implements OnDestroy {
    private readonly zone = inject(NgZone);
    private readonly dialogService = inject(DialogService);
    private readonly dotMessageService = inject(DotMessageService);

    /** Live ref for the image picker; cleared when the dialog closes or the service tears down. */
    private imagePickerRef: DynamicDialogRef | null = null;

    /** Live ref for the video picker; cleared when the dialog closes or the service tears down. */
    private videoPickerRef: DynamicDialogRef | null = null;

    /** Live ref for the audio picker; cleared when the dialog closes or the service tears down. */
    private audioPickerRef: DynamicDialogRef | null = null;

    /**
     * Open state for the AI Image prompt modal. Tracking it as a signal lets other parts
     * of the editor know an AI Image dialog is active without poking the live ref.
     */
    readonly aiImageOpen = signal(false);

    /** Live PrimeNG dialog ref for the AI image prompt; cleared on close / destroy. */
    private aiImageDialogRef: DynamicDialogRef | null = null;

    /** Live PrimeNG dialog ref for the AI content prompt; cleared on close / destroy. */
    private aiContentDialogRef: DynamicDialogRef | null = null;

    /**
     * Opens {@link DotBrowserSelectorComponent} scoped to image-mime contentlets. On accept,
     * inserts the picked contentlet as a `dotImage` node at the editor's current selection.
     * Idempotent: a second call while the picker is already open is a no-op.
     */
    openImagePicker(editor: Editor): void {
        if (this.imagePickerRef) return;

        this.imagePickerRef = this.dialogService.open(
            DotBrowserSelectorComponent,
            buildBrowserSelectorConfig({
                header: this.dotMessageService.get(
                    'dot.block-editor.extension.image.dotcms.dialog-title'
                ),
                mimeTypes: ['image']
            })
        );

        this.imagePickerRef.onClose.subscribe((contentlet?: DotCMSContentlet) => {
            if (contentlet) {
                this.zone.run(() => insertDotImageFromContentlet(editor, contentlet));
            }
            this.imagePickerRef = null;
        });
    }

    /**
     * Opens {@link DotBrowserSelectorComponent} scoped to video-mime contentlets. On accept,
     * inserts the picked contentlet as a `dotVideo` node at the editor's current selection.
     * Idempotent: a second call while the picker is already open is a no-op.
     */
    openVideoPicker(editor: Editor): void {
        if (this.videoPickerRef) return;

        this.videoPickerRef = this.dialogService.open(
            DotBrowserSelectorComponent,
            buildBrowserSelectorConfig({
                header: this.dotMessageService.get(
                    'dot.block-editor.extension.video.dotcms.dialog-title'
                ),
                mimeTypes: ['video']
            })
        );

        this.videoPickerRef.onClose.subscribe((contentlet?: DotCMSContentlet) => {
            if (contentlet) {
                this.zone.run(() => insertDotVideoFromContentlet(editor, contentlet));
            }
            this.videoPickerRef = null;
        });
    }

    /**
     * Opens {@link DotBrowserSelectorComponent} scoped to audio-mime contentlets. On accept,
     * inserts the picked contentlet as a `dotAudio` node at the editor's current selection.
     * Idempotent: a second call while the picker is already open is a no-op.
     */
    openAudioPicker(editor: Editor): void {
        if (this.audioPickerRef) return;

        this.audioPickerRef = this.dialogService.open(
            DotBrowserSelectorComponent,
            buildBrowserSelectorConfig({
                header: this.dotMessageService.get(
                    'dot.block-editor.extension.audio.dotcms.dialog-title'
                ),
                mimeTypes: ['audio']
            })
        );

        this.audioPickerRef.onClose.subscribe((contentlet?: DotCMSContentlet) => {
            if (contentlet) {
                this.zone.run(() => insertDotAudioFromContentlet(editor, contentlet));
            }
            this.audioPickerRef = null;
        });
    }

    /**
     * Opens the AI Image prompt dialog ({@link DotAIImagePromptComponent} from `@dotcms/ui`).
     * On close, if the user accepted a generated image, inserts it as a `dotImage` node at
     * the editor's current selection. Closing without a selection (cancel/discard) is a
     * no-op other than clearing local state.
     */
    openAiImage(editor: Editor): void {
        if (this.aiImageDialogRef) return;

        this.zone.run(() => this.aiImageOpen.set(true));

        this.aiImageDialogRef = this.dialogService.open(DotAIImagePromptComponent, {
            header: this.dotMessageService.get('block-editor.extension.ai-image.dialog-title'),
            appendTo: 'body',
            // Modal must clear the fullscreen editor shell's `z-[9998]` backdrop.
            baseZIndex: OVERLAY_ABOVE_FULLSCREEN_Z_INDEX,
            closeOnEscape: true,
            closable: true,
            dismissableMask: true,
            draggable: false,
            keepInViewport: false,
            resizable: false,
            modal: true,
            width: '90%',
            style: { 'max-width': '1040px' },
            data: { context: editor.getText() }
        });

        this.aiImageDialogRef.onClose.subscribe((selectedImage?: DotGeneratedAIImage) => {
            if (selectedImage?.response?.contentlet) {
                this.zone.run(() =>
                    insertDotImageFromContentlet(editor, selectedImage.response.contentlet)
                );
            }
            this.aiImageDialogRef = null;
            this.zone.run(() => this.aiImageOpen.set(false));
        });
    }

    /**
     * Imperatively closes the AI Image prompt dialog. The dialog's own `onClose` subscription
     * resets the rest of the state, so we do not have to flip {@link aiImageOpen} here.
     */
    closeAiImage(): void {
        this.aiImageDialogRef?.close();
    }

    /**
     * Opens the AI Content prompt dialog ({@link AiContentDialogComponent}). On accept,
     * inserts the generated HTML as an `aiContent` node at the editor's current selection.
     * Cancel / Discard / Escape / X close with no value and no insertion.
     * Idempotent: a second call while the dialog is already open is a no-op.
     */
    openAiContent(editor: Editor): void {
        if (this.aiContentDialogRef) return;

        this.aiContentDialogRef = this.dialogService.open(AiContentDialogComponent, {
            header: this.dotMessageService.get('dot.block.editor.dialog.ai-content.header'),
            appendTo: 'body',
            // Modal must clear the fullscreen editor shell's `z-[9998]` backdrop.
            baseZIndex: OVERLAY_ABOVE_FULLSCREEN_Z_INDEX,
            closeOnEscape: true,
            closable: true,
            // Match the original embedded behavior — clicking outside should NOT discard
            // an in-flight prompt or generated draft.
            dismissableMask: false,
            draggable: false,
            resizable: false,
            modal: true,
            width: '720px',
            style: { 'max-width': '90vw' }
        });

        this.aiContentDialogRef.onClose.subscribe((html?: string) => {
            if (html) {
                this.zone.run(() => editor.chain().focus().insertContent(html).run());
            }
            this.aiContentDialogRef = null;
        });
    }

    ngOnDestroy(): void {
        this.imagePickerRef?.close();
        this.imagePickerRef = null;
        this.videoPickerRef?.close();
        this.videoPickerRef = null;
        this.audioPickerRef?.close();
        this.audioPickerRef = null;
        this.aiImageDialogRef?.close();
        this.aiImageDialogRef = null;
        this.aiContentDialogRef?.close();
        this.aiContentDialogRef = null;
    }
}
