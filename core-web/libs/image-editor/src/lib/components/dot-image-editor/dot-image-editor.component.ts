import { injectDispatch } from '@ngrx/signals/events';

import { DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { Dialog } from 'primeng/dialog';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';

import { imageEditorModalScaleFade } from '../../animations/image-editor.animations';
import { DIALOG_SIZE_TRANSITION, FULLSCREEN_DIALOG_STYLE } from '../../image-editor.constants';
import { ImageEditorOpenParams } from '../../models/image-editor.models';
import {
    imageEditorHistoryEvents,
    imageEditorLifecycleEvents
} from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';
import { DotImageEditorCanvasComponent } from '../dot-image-editor-canvas/dot-image-editor-canvas.component';
import { DotImageEditorFooterComponent } from '../dot-image-editor-footer/dot-image-editor-footer.component';
import { DotImageEditorHeaderComponent } from '../dot-image-editor-header/dot-image-editor-header.component';
import { DotImageEditorPanelsComponent } from '../dot-image-editor-panels/dot-image-editor-panels.component';

/**
 * Full-screen "Edit image" modal shell, opened through PrimeNG's `DialogService`.
 * Assembles the header, canvas, side panels and footer over a single
 * {@link ImageEditorStore} instance scoped to this dialog. It owns the dialog
 * lifecycle: it requests the asset on init, closes with the saved temp file when
 * a save succeeds, and guards close/cancel against unsaved edits.
 */
@Component({
    selector: 'dot-image-editor',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        ConfirmDialogModule,
        DotImageEditorHeaderComponent,
        DotImageEditorCanvasComponent,
        DotImageEditorPanelsComponent,
        DotImageEditorFooterComponent
    ],
    providers: [
        ImageEditorStore,
        // Scoped here so the template's `<p-confirmDialog />` and the discard
        // guard share one instance; the lib has no global confirm dialog.
        ConfirmationService
    ],
    templateUrl: './dot-image-editor.component.html',
    styleUrl: './dot-image-editor.component.scss',
    animations: [imageEditorModalScaleFade()],
    host: {
        '[@imageEditorModalScaleFade]': '',
        // Listen on the document, not the host: in a DynamicDialog focus usually
        // sits outside this component, so a host-only keydown never fires.
        '(document:keydown)': 'onKeydown($event)'
    }
})
export class DotImageEditorComponent {
    /** Image editor state store, isolated to this dialog instance. */
    protected readonly store = inject(ImageEditorStore);

    readonly #config = inject<DynamicDialogConfig<ImageEditorOpenParams>>(DynamicDialogConfig);
    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dispatch = injectDispatch(imageEditorLifecycleEvents);
    readonly #historyDispatch = injectDispatch(imageEditorHistoryEvents);
    readonly #document = inject(DOCUMENT);
    // The PrimeNG dialog hosting this editor: injectable because the dialog content
    // is declared inside `<p-dialog>` in DynamicDialog's template, so we sit in the
    // Dialog's element injector. Its `container()` signal is the `.p-dialog` element.
    readonly #dialog = inject(Dialog, { optional: true });

    /** Windowed inline dialog styles saved on entering full-screen, restored on exit. */
    #windowedStyle: Record<string, string> | null = null;

    constructor() {
        this.#dispatch.assetRequested(this.#config.data as ImageEditorOpenParams);

        // The editor owns its dialog, so it owns full-screen too: resize the host
        // `.p-dialog` to fill the viewport whenever `isFullscreen` flips.
        effect(() => this.#applyFullscreen(this.store.isFullscreen()));
    }

    /**
     * Expands the host dialog to the viewport (or restores it). `DialogService`
     * sets the dialog width/height as inline styles, so we override those inline
     * styles directly — a stylesheet rule can't win against inline without
     * `!important` — and restore the saved values on exit.
     */
    #applyFullscreen(on: boolean): void {
        const dialog = this.#dialog?.container() as HTMLElement | undefined;

        if (!dialog) {
            return;
        }

        // Set the size transition (idempotent) before any toggle, honouring
        // reduced-motion. It lands on the first (windowed) effect run, so the
        // first real toggle already animates.
        dialog.style.transition = this.#prefersReducedMotion() ? '' : DIALOG_SIZE_TRANSITION;

        if (on) {
            this.#windowedStyle ??= Object.fromEntries(
                Object.keys(FULLSCREEN_DIALOG_STYLE).map((prop) => [prop, dialog.style[prop]])
            );
            Object.assign(dialog.style, FULLSCREEN_DIALOG_STYLE);
        } else if (this.#windowedStyle) {
            Object.assign(dialog.style, this.#windowedStyle);
            this.#windowedStyle = null;
        }
    }

    /** Whether the user has requested reduced motion (skips the resize animation). */
    #prefersReducedMotion(): boolean {
        return (
            this.#document.defaultView?.matchMedia?.('(prefers-reduced-motion: reduce)').matches ??
            false
        );
    }

    /**
     * Handles the standard undo/redo shortcuts while the editor is focused:
     * Ctrl/Cmd+Z undoes, Ctrl/Cmd+Shift+Z and Ctrl/Cmd+Y redo. Skipped when a
     * text field has focus so its native text undo keeps working.
     */
    protected onKeydown(event: KeyboardEvent): void {
        if (!(event.metaKey || event.ctrlKey) || this.#isEditableTarget(event.target)) {
            return;
        }

        const key = event.key.toLowerCase();
        const isUndo = key === 'z' && !event.shiftKey;
        const isRedo = key === 'y' || (key === 'z' && event.shiftKey);

        if (!isUndo && !isRedo) {
            return;
        }

        event.preventDefault();

        if (isUndo && this.store.canUndo()) {
            this.#historyDispatch.undoRequested();
        } else if (isRedo && this.store.canRedo()) {
            this.#historyDispatch.redoRequested();
        }
    }

    /** Closes the editor, confirming first when there are unsaved edits. */
    protected requestClose(): void {
        if (!this.store.isDirty()) {
            this.#dialogRef.close(null);

            return;
        }

        this.#confirmationService.confirm({
            header: this.#dotMessageService.get('edit.content.image-editor.discard.header'),
            message: this.#dotMessageService.get('edit.content.image-editor.discard.message'),
            acceptLabel: this.#dotMessageService.get('edit.content.image-editor.discard.confirm'),
            rejectLabel: this.#dotMessageService.get('edit.content.image-editor.discard.reject'),
            accept: () => this.#dialogRef.close(null)
        });
    }

    /** Whether the event originated from an editable control (keeps its native undo). */
    #isEditableTarget(target: EventTarget | null): boolean {
        const el = target as HTMLElement | null;

        if (!el) {
            return false;
        }

        return (
            el.tagName === 'INPUT' ||
            el.tagName === 'TEXTAREA' ||
            el.tagName === 'SELECT' ||
            el.isContentEditable
        );
    }
}
