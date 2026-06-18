import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { DotCMSTempFile } from '@dotcms/dotcms-models';
import { DotImageEditorLauncher, ImageEditorOpenParams } from '@dotcms/image-editor';

/** Detail of the `binaryField-tempfile-{variable}` custom event. */
interface TempFileEventDetail {
    tempFile: DotCMSTempFile;
}

/**
 * Bridges the editor launcher contract to the existing legacy Dojo image editor.
 *
 * Reuses the established document `CustomEvent` channel: it dispatches
 * `binaryField-open-image-editor-{variable}` to open the editor and resolves the
 * result from `binaryField-tempfile-{variable}` (edited image) or
 * `binaryField-close-image-editor-{variable}` (cancelled).
 */
@Injectable()
export class LegacyDojoImageEditorLauncher implements DotImageEditorLauncher {
    isAvailable(): boolean {
        return true;
    }

    /**
     * Opens the legacy image editor for the given asset.
     *
     * @param params - Identifiers and metadata of the asset to edit
     * @returns Emits the edited temp file, or `null` if the user cancelled
     */
    open(params: ImageEditorOpenParams): Observable<DotCMSTempFile | null> {
        const { inode, tempId, variable } = params;
        const tempFileEventName = `binaryField-tempfile-${variable}`;
        const closeEventName = `binaryField-close-image-editor-${variable}`;

        return new Observable<DotCMSTempFile | null>((subscriber) => {
            const handleTempFile = (event: Event): void => {
                const { detail } = event as CustomEvent<TempFileEventDetail>;
                subscriber.next(detail?.tempFile ?? null);
                subscriber.complete();
            };

            const handleClose = (): void => {
                subscriber.next(null);
                subscriber.complete();
            };

            document.addEventListener(tempFileEventName, handleTempFile);
            document.addEventListener(closeEventName, handleClose);

            document.dispatchEvent(
                new CustomEvent(`binaryField-open-image-editor-${variable}`, {
                    detail: { inode, tempId, variable }
                })
            );

            return () => {
                document.removeEventListener(tempFileEventName, handleTempFile);
                document.removeEventListener(closeEventName, handleClose);
            };
        });
    }
}
