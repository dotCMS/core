import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { DotCMSTempFile } from '@dotcms/dotcms-models';

import { ImageEditorLauncher, ImageEditorOpenParams } from './image-editor-launcher.model';

/** Detail of the `binaryField-tempfile-{variable}` custom event. */
interface ImageEditorTempFileDetail {
    tempFile: DotCMSTempFile;
}

/**
 * {@link ImageEditorLauncher} backed by the legacy Dojo image editor.
 *
 * Used by the `dotcms-binary-field` web component embedded in the legacy JSP
 * editor. It dispatches the `binaryField-open-image-editor-{variable}` custom
 * event the JSP listens for (which opens the Dojo `ImageEditor`), then resolves
 * with the edited temp file when the editor emits `binaryField-tempfile-{variable}`.
 *
 * The session completes (without emitting) when the editor dispatches
 * `binaryField-close-image-editor-{variable}`.
 */
@Injectable()
export class LegacyDojoImageEditorLauncher implements ImageEditorLauncher {
    isAvailable(): boolean {
        return true;
    }

    open({ inode, tempId, variable }: ImageEditorOpenParams): Observable<DotCMSTempFile> {
        return new Observable<DotCMSTempFile>((subscriber) => {
            const tempFileEventName = `binaryField-tempfile-${variable}`;
            const closeEventName = `binaryField-close-image-editor-${variable}`;

            const onTempFile = (event: Event) => {
                const { tempFile } = (event as CustomEvent<ImageEditorTempFileDetail>).detail ?? {};

                if (tempFile) {
                    subscriber.next(tempFile);
                }

                subscriber.complete();
            };

            const onClose = () => subscriber.complete();

            document.addEventListener(tempFileEventName, onTempFile);
            document.addEventListener(closeEventName, onClose);

            document.dispatchEvent(
                new CustomEvent(`binaryField-open-image-editor-${variable}`, {
                    detail: { inode, tempId, variable }
                })
            );

            return () => {
                document.removeEventListener(tempFileEventName, onTempFile);
                document.removeEventListener(closeEventName, onClose);
            };
        });
    }
}
