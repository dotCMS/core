import { Observable, map, take } from 'rxjs';

import { inject, Injectable } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { DialogService } from 'primeng/dynamicdialog';

import { DotPropertiesService } from '@dotcms/data-access';
import { DotCMSTempFile, FeaturedFlags } from '@dotcms/dotcms-models';
import {
    DotImageEditorComponent,
    DotImageEditorLauncher,
    ImageEditorOpenParams
} from '@dotcms/image-editor';

/**
 * Launches the Angular `@dotcms/image-editor` modal through PrimeNG's `DialogService`.
 *
 * Gated behind the {@link FeaturedFlags.FEATURE_FLAG_NEW_IMAGE_EDITOR} feature flag:
 * `isAvailable()` resolves to the server-configured value, which ships as `false`
 * for rollback safety (mirroring the other new-editor flags), so the binary field
 * falls back to the legacy Dojo editor until an admin enables it.
 */
@Injectable()
export class AngularImageEditorLauncher implements DotImageEditorLauncher {
    readonly #dialogService = inject(DialogService);
    readonly #propertiesService = inject(DotPropertiesService);

    /** Resolved value of the new-image-editor feature flag (off until the server replies). */
    readonly #enabled = toSignal(
        this.#propertiesService.getFeatureFlag(FeaturedFlags.FEATURE_FLAG_NEW_IMAGE_EDITOR),
        { initialValue: false }
    );

    isAvailable(): boolean {
        return this.#enabled();
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
            // (DotImageEditorHeaderComponent) and the Esc key.
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
            // Esc is handled inside the editor (routed through its unsaved-changes guard),
            // not by PrimeNG — closeOnEscape would close the dialog directly and skip the
            // discard confirmation.
            closeOnEscape: false,
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
