import { Injectable, NgZone, OnDestroy, inject, signal } from '@angular/core';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { Editor } from '@tiptap/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotGeneratedAIImage } from '@dotcms/dotcms-models';
import { DotAIImagePromptComponent } from '@dotcms/ui';

import { insertDotImageFromContentlet } from '../editor.utils';

export type DialogId = 'image' | 'image-properties' | 'link' | 'table' | 'video' | 'emoji';

/** Prefill payload for the {@link ImagePropertiesDialogComponent} (edit-mode). */
export interface ImagePropertiesPayload {
    initialValues: { src: string; title: string; alt: string };
}

export interface LinkDialogPayload {
    initialValues?: { href?: string; displayText?: string; target?: string | null };
    /** The anchor element whose link is being edited — gets the `link-editing` CSS class. */
    linkEl?: HTMLElement;
    /** Pre-computed position for edit-mode insertions — captured at open time. */
    anchorPos?: number;
}

interface ActiveDialog {
    id: DialogId;
    clientRectFn: () => DOMRect | null;
}

@Injectable()
export class EditorDialogManagerService implements OnDestroy {
    private readonly zone = inject(NgZone);
    private readonly dialogService = inject(DialogService);
    private readonly dotMessageService = inject(DotMessageService);

    readonly activeDialog = signal<ActiveDialog | null>(null);
    readonly imagePropertiesPayload = signal<ImagePropertiesPayload | null>(null);
    readonly linkPayload = signal<LinkDialogPayload | null>(null);

    /**
     * AI Content dialog uses a centered PrimeNG modal (large content with preview area),
     * not the caret-anchored `<dot-editor-dialog>` shell. State lives outside `activeDialog`
     * because it doesn't need a position rect.
     */
    readonly aiContentOpen = signal(false);

    /**
     * AI Image dialog also uses a centered modal — the {@link DotAIImagePromptComponent}
     * UI from `@dotcms/ui` is opened via PrimeNG's {@link DialogService.open}, which means
     * we cannot embed it as a normal Angular template. Tracking the open state as a signal
     * lets the rest of the editor know a modal is active without poking the dialog ref.
     */
    readonly aiImageOpen = signal(false);

    /** Live PrimeNG dialog ref for the AI image prompt; cleared on close / destroy. */
    private aiImageDialogRef: DynamicDialogRef | null = null;

    isOpen(id: DialogId): boolean {
        return this.activeDialog()?.id === id;
    }

    /** Opens a dialog with no payload (image insert, table, video, emoji). */
    open(id: DialogId, clientRectFn: () => DOMRect | null): void {
        this.zone.run(() => this.activeDialog.set({ id, clientRectFn }));
    }

    /**
     * Opens the {@link ImageInsertDialogComponent} (three-tab picker — Upload / URL / dotCMS).
     * No prefill; this entry point is for inserting a brand-new image at the caret.
     */
    openImage(clientRectFn: () => DOMRect | null): void {
        this.zone.run(() => this.activeDialog.set({ id: 'image', clientRectFn }));
    }

    /**
     * Opens the {@link ImagePropertiesDialogComponent} (edit-mode flat src / title / alt form)
     * with the given payload prefilled. Triggered from the toolbar's "Edit image properties"
     * button when a `dotImage` node is selected.
     */
    openImageProperties(
        clientRectFn: () => DOMRect | null,
        payload: ImagePropertiesPayload
    ): void {
        this.zone.run(() => {
            this.imagePropertiesPayload.set(payload);
            this.activeDialog.set({ id: 'image-properties', clientRectFn });
        });
    }

    // TODO: Make the payload part of the open method and make the dialog component have a beforeShow that will be a callback that will receive the payload
    // That way we avoid creating custom function for complicated dialogs like the link

    openLink(clientRectFn: () => DOMRect | null, payload?: LinkDialogPayload): void {
        this.zone.run(() => {
            this.linkPayload.set(payload ?? null);
            this.activeDialog.set({ id: 'link', clientRectFn });
        });
    }

    close(): void {
        this.zone.run(() => this.activeDialog.set(null));
    }

    openAiContent(): void {
        this.zone.run(() => this.aiContentOpen.set(true));
    }

    closeAiContent(): void {
        this.zone.run(() => this.aiContentOpen.set(false));
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
            closeOnEscape: false,
            draggable: false,
            keepInViewport: false,
            maskStyleClass: 'p-dialog-mask-transparent-ai',
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

    ngOnDestroy(): void {
        this.aiImageDialogRef?.close();
        this.aiImageDialogRef = null;
    }

    /** Toggle: if the same dialog is already open, close it; otherwise open it. */
    toggle(id: DialogId, clientRectFn: () => DOMRect | null): void {
        this.activeDialog()?.id === id ? this.close() : this.open(id, clientRectFn);
    }
}
