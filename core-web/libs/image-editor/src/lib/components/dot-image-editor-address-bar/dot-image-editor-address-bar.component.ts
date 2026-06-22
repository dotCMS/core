import { injectDispatch } from '@ngrx/signals/events';

import { DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { imageEditorHistoryEvents, imageEditorViewEvents } from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';

/**
 * Address sub-bar shown on the dark canvas. Surfaces the cache-busted preview
 * URL with a copy action on the left, and zoom, full-screen and undo/redo
 * controls on the right. Zoom is emitted as outputs (`zoomIn`/`zoomOut`/`fit`)
 * for the canvas to apply a CSS transform; undo/redo dispatch
 * {@link imageEditorHistoryEvents} and the full-screen toggle dispatches
 * {@link imageEditorViewEvents}. The root component owns the actual dialog
 * resize, reacting to `store.isFullscreen()`.
 */
@Component({
    selector: 'dot-image-editor-address-bar',
    templateUrl: './dot-image-editor-address-bar.component.html',
    styleUrl: './dot-image-editor-address-bar.component.scss',
    imports: [ButtonModule, TooltipModule, DotMessagePipe],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotImageEditorAddressBarComponent {
    protected readonly store = inject(ImageEditorStore);
    readonly #dispatch = injectDispatch(imageEditorHistoryEvents);
    readonly #viewDispatch = injectDispatch(imageEditorViewEvents);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #document = inject(DOCUMENT);

    /** Current canvas zoom percentage to display; owned and updated by the canvas. */
    zoomLevel = input<number>(100);

    /** Emitted when the user requests to zoom the canvas in. */
    zoomIn = output<void>();
    /** Emitted when the user requests to zoom the canvas out. */
    zoomOut = output<void>();
    /** Emitted when the user requests to fit the image to the viewport. */
    fit = output<void>();

    /** Copies the current preview URL to the clipboard, surfacing a toast. */
    protected async copyUrl(): Promise<void> {
        try {
            await navigator.clipboard.writeText(this.#absoluteUrl());
            this.#messageService.add({
                severity: 'success',
                detail: this.#dotMessageService.get(
                    'edit.content.image-editor.address.copy.success'
                )
            });
        } catch {
            // Clipboard access can be denied; copy failure is non-fatal.
            this.#messageService.add({
                severity: 'error',
                detail: this.#dotMessageService.get('edit.content.image-editor.address.copy.error')
            });
        }
    }

    /**
     * Resolves the store's root-relative preview URL against the current origin so the
     * copied value is a complete, shareable URL rather than just the path.
     */
    #absoluteUrl(): string {
        const url = this.store.previewUrl();
        const origin = this.#document.location?.origin ?? '';

        return /^https?:\/\//.test(url) ? url : `${origin}${url}`;
    }

    /** Toggles the editor dialog between its windowed size and full-screen. */
    protected toggleFullscreen(): void {
        this.#viewDispatch.fullscreenToggled();
    }

    /** Steps back one entry in the edit history. */
    protected undo(): void {
        this.#dispatch.undoRequested();
    }

    /** Steps forward one entry in the edit history. */
    protected redo(): void {
        this.#dispatch.redoRequested();
    }
}
