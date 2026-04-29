import { Injectable, NgZone, inject, signal } from '@angular/core';

export type DialogId = 'image' | 'link' | 'table' | 'video' | 'emoji';

export interface ImageDialogPayload {
    initialValues?: { src: string; title: string; alt: string };
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
    readonly imagePayload = signal<ImageDialogPayload | null>(null);
    readonly linkPayload = signal<LinkDialogPayload | null>(null);

    /**
     * AI Content dialog uses a centered PrimeNG modal (large content with preview area),
     * not the caret-anchored `<dot-editor-dialog>` shell. State lives outside `activeDialog`
     * because it doesn't need a position rect.
     */
    readonly aiContentOpen = signal(false);

    isOpen(id: DialogId): boolean {
        return this.activeDialog()?.id === id;
    }

    /** Opens a dialog with no payload (table, video, emoji). */
    open(id: DialogId, clientRectFn: () => DOMRect | null): void {
        this.zone.run(() => this.activeDialog.set({ id, clientRectFn }));
    }

    // TODO: Make the payload part of the open method and make the dialog component have a beforeShow that will be a callback that will receive the payload
    // That way we avoid creating custom function for complicated dialogs like the image and link
    openImage(clientRectFn: () => DOMRect | null, payload?: ImageDialogPayload): void {
        this.zone.run(() => {
            this.imagePayload.set(payload ?? null);
            this.activeDialog.set({ id: 'image', clientRectFn });
        });
    }

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
