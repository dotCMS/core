import { injectDispatch } from '@ngrx/signals/events';

import { DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { ActiveTool, ToolRailItem } from '../../models/image-editor.models';
import { imageEditorHistoryEvents, imageEditorToolEvents } from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';

/**
 * Address sub-bar shown on the canvas, laid out as three rounded "pills" (mirroring
 * the UVE editor toolbar). Left to right: the canvas tools (move/grab + crop), the
 * cache-busted preview URL with a copy action, and the zoom + undo/redo controls.
 * Selecting a tool dispatches {@link imageEditorToolEvents} so the store owns the
 * active tool; zoom is emitted as outputs (`zoomIn`/`zoomOut`/`fit`) for the canvas
 * to apply a CSS transform; undo/redo dispatch {@link imageEditorHistoryEvents}.
 * (The full-screen toggle lives in the dialog header, next to the close button.)
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
    readonly #toolDispatch = injectDispatch(imageEditorToolEvents);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #document = inject(DOCUMENT);

    /** The ordered canvas tools rendered as toggles in the leftmost pill. */
    protected readonly tools: ToolRailItem[] = [
        {
            id: 'move',
            label: 'edit.content.image-editor.tool.move',
            testId: 'image-editor-tool-move'
        },
        {
            id: 'crop',
            label: 'edit.content.image-editor.tool.crop',
            testId: 'image-editor-tool-crop'
        }
    ];

    /** Selects a canvas tool, delegating the active-tool state change to the store. */
    protected selectTool(tool: ActiveTool): void {
        this.#toolDispatch.toolSelected(tool);
    }

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

    /** Steps back one entry in the edit history. */
    protected undo(): void {
        this.#dispatch.undoRequested();
    }

    /** Steps forward one entry in the edit history. */
    protected redo(): void {
        this.#dispatch.redoRequested();
    }
}
