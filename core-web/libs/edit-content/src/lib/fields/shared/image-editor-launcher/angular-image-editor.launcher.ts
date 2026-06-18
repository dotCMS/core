import { Observable, map, take } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';
import {
    DotImageEditorComponent,
    DotImageEditorLauncher,
    ImageEditorOpenParams
} from '@dotcms/image-editor';

/**
 * Launches the Angular `@dotcms/image-editor` modal through PrimeNG's `DialogService`.
 */
@Injectable()
export class AngularImageEditorLauncher implements DotImageEditorLauncher {
    readonly #dialogService = inject(DialogService);
    readonly #dotMessageService = inject(DotMessageService);

    isAvailable(): boolean {
        return true;
    }

    /**
     * Opens the image editor dialog for the given asset.
     *
     * @param params - Identifiers and metadata of the asset to edit
     * @returns Emits the saved temp file, or `null` if the user cancelled
     */
    open(params: ImageEditorOpenParams): Observable<DotCMSTempFile | null> {
        const ref = this.#dialogService.open(DotImageEditorComponent, {
            // The editor renders its own header (title + close ✕), so hide PrimeNG's
            // chrome header to avoid a duplicate. Closing is handled by the internal ✕
            // (DotImageEditorHeaderComponent) and the Esc key (closeOnEscape).
            showHeader: false,
            data: params,
            // Large landscape dialog: wider than tall. Generous caps keep it big on wide
            // screens while staying within the viewport on smaller ones.
            width: 'min(96vw, 90rem)',
            height: 'min(96vh, 60rem)',
            modal: true,
            draggable: false,
            resizable: false,
            closable: true,
            closeOnEscape: true,
            dismissableMask: false,
            contentStyle: { height: '100%', overflow: 'hidden', padding: '0' },
            styleClass: 'dot-image-editor-dialog'
        });

        return ref.onClose.pipe(
            map((tempFile?: DotCMSTempFile) => tempFile ?? null),
            take(1)
        );
    }
}
