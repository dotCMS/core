import { injectDispatch } from '@ngrx/signals/events';

import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';

import { imageEditorModalScaleFade } from '../../animations/image-editor.animations';
import { ImageEditorOpenParams } from '../../models/image-editor.models';
import { imageEditorLifecycleEvents } from '../../store/image-editor.events';
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
        '[@imageEditorModalScaleFade]': ''
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

    constructor() {
        this.#dispatch.assetRequested(this.#config.data as ImageEditorOpenParams);

        // Close the dialog with the saved file the moment a save succeeds.
        effect(() => {
            const savedTempFile = this.store.savedTempFile();

            if (savedTempFile) {
                this.#dialogRef.close(savedTempFile);
            }
        });
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
}
