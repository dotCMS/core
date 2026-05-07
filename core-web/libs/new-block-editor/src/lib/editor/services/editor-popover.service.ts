import { Injectable, NgZone, inject, signal } from '@angular/core';

export type PopoverId = 'image-properties' | 'link' | 'table' | 'emoji' | 'asset-by-url';

/** Prefill payload for the {@link ImagePropertiesPopoverComponent} (edit-mode). */
export interface ImagePropertiesPayload {
    initialValues: { src: string; title: string; alt: string };
}

export interface LinkPopoverPayload {
    initialValues?: {
        href?: string;
        displayText?: string;
        target?: string | null;
        title?: string | null;
        ariaLabel?: string | null;
        rel?: string | null;
    };
    /** The anchor element whose link is being edited — gets the `link-editing` CSS class. */
    linkEl?: HTMLElement;
    /** Pre-computed position for edit-mode insertions — captured at open time. */
    anchorPos?: number;
}

interface ActivePopover {
    id: PopoverId;
    clientRectFn: () => DOMRect | null;
}

/**
 * Owns the state for caret-anchored editor popovers (link, image-properties, table, emoji).
 * Each popover is rendered through the shared `<dot-editor-popover>` shell, which subscribes
 * to {@link activePopover} and positions itself against the trigger rect via floating-ui.
 *
 * Sibling to {@link EditorModalService}, which owns centered modal dialogs (AI content,
 * AI image, image picker, video picker).
 */
@Injectable()
export class EditorPopoverService {
    private readonly zone = inject(NgZone);

    readonly activePopover = signal<ActivePopover | null>(null);
    readonly imagePropertiesPayload = signal<ImagePropertiesPayload | null>(null);
    readonly linkPayload = signal<LinkPopoverPayload | null>(null);

    /**
     * **Reactive:** reads {@link activePopover}, so calling this from inside an `effect()`
     * or a `computed()` will re-run when any popover opens or closes. Returns `true` iff the
     * given popover id matches the currently active one.
     */
    isOpen(id: PopoverId): boolean {
        return this.activePopover()?.id === id;
    }

    /** Opens a caret-anchored popover with no payload (table, emoji). */
    open(id: PopoverId, clientRectFn: () => DOMRect | null): void {
        this.zone.run(() => this.activePopover.set({ id, clientRectFn }));
    }

    /**
     * Opens the {@link ImagePropertiesPopoverComponent} (edit-mode flat src / title / alt form)
     * with the given payload prefilled. Triggered from the toolbar's "Edit image properties"
     * button when a `dotImage` node is selected.
     */
    openImageProperties(clientRectFn: () => DOMRect | null, payload: ImagePropertiesPayload): void {
        this.zone.run(() => {
            this.imagePropertiesPayload.set(payload);
            this.activePopover.set({ id: 'image-properties', clientRectFn });
        });
    }

    // TODO: Make the payload part of the open method and make the popover component have a
    // beforeShow callback that receives the payload — avoids creating bespoke openX methods.

    openLink(clientRectFn: () => DOMRect | null, payload?: LinkPopoverPayload): void {
        this.zone.run(() => {
            this.linkPayload.set(payload ?? null);
            this.activePopover.set({ id: 'link', clientRectFn });
        });
    }

    close(): void {
        this.zone.run(() => this.activePopover.set(null));
    }

    /** Toggle: if the same popover is already open, close it; otherwise open it. */
    toggle(id: PopoverId, clientRectFn: () => DOMRect | null): void {
        this.activePopover()?.id === id ? this.close() : this.open(id, clientRectFn);
    }
}
