import { Injectable, NgZone, inject, signal } from '@angular/core';

export type DialogId = 'image-properties' | 'link' | 'table' | 'emoji';

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
export class EditorDialogManagerService {
    private readonly zone = inject(NgZone);

    readonly activeDialog = signal<ActiveDialog | null>(null);
    readonly imagePropertiesPayload = signal<ImagePropertiesPayload | null>(null);
    readonly linkPayload = signal<LinkDialogPayload | null>(null);

    /**
     * AI Content dialog uses a centered PrimeNG modal (large content with preview area),
     * not the caret-anchored `<dot-editor-dialog>` shell. State lives outside `activeDialog`
     * because it doesn't need a position rect. The AI Image counterpart lives on
     * {@link EditorModalService} because it opens via `DialogService.open()`.
     */
    readonly aiContentOpen = signal(false);

    isOpen(id: DialogId): boolean {
        return this.activeDialog()?.id === id;
    }

    /** Opens a caret-anchored dialog with no payload (table, emoji). */
    open(id: DialogId, clientRectFn: () => DOMRect | null): void {
        this.zone.run(() => this.activeDialog.set({ id, clientRectFn }));
    }

    /**
     * Opens the {@link ImagePropertiesDialogComponent} (edit-mode flat src / title / alt form)
     * with the given payload prefilled. Triggered from the toolbar's "Edit image properties"
     * button when a `dotImage` node is selected.
     */
    openImageProperties(clientRectFn: () => DOMRect | null, payload: ImagePropertiesPayload): void {
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

    /** Toggle: if the same dialog is already open, close it; otherwise open it. */
    toggle(id: DialogId, clientRectFn: () => DOMRect | null): void {
        this.activeDialog()?.id === id ? this.close() : this.open(id, clientRectFn);
    }
}
