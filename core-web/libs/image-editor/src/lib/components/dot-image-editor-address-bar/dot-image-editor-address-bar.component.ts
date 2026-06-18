import { injectDispatch } from '@ngrx/signals/events';

import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { imageEditorHistoryEvents } from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';

/**
 * Address sub-bar shown on the dark canvas. Surfaces the cache-busted preview
 * URL with a copy action on the left, and zoom plus undo/redo controls on the
 * right. Zoom is emitted as outputs (`zoomIn`/`zoomOut`/`fit`) for the canvas to
 * apply a CSS transform, since the store has no zoom events yet; undo and redo
 * dispatch {@link imageEditorHistoryEvents} so the store owns history.
 */
@Component({
    selector: 'dot-image-editor-address-bar',
    templateUrl: './dot-image-editor-address-bar.component.html',
    styleUrl: './dot-image-editor-address-bar.component.scss',
    imports: [ButtonModule, InputTextModule, TooltipModule, DotMessagePipe],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotImageEditorAddressBarComponent {
    protected readonly store = inject(ImageEditorStore);
    readonly #dispatch = injectDispatch(imageEditorHistoryEvents);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);

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
            await navigator.clipboard.writeText(this.store.previewUrl());
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

    /** Steps back one entry in the edit history. */
    protected undo(): void {
        this.#dispatch.undoRequested();
    }

    /** Steps forward one entry in the edit history. */
    protected redo(): void {
        this.#dispatch.redoRequested();
    }
}
