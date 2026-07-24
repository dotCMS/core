import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';

import {
    DotLegacyImageEditorDialogComponent,
    DotLegacyImageEditorDialogData
} from './dot-legacy-image-editor-dialog.component';
import { ImageEditorLauncher, ImageEditorOpenParams } from './image-editor-launcher.model';

/** Shape of the `postMessage` payloads forwarded by `image-editor-standalone.jsp`. */
interface ImageEditorMessage {
    source?: string;
    type?: 'tempfile' | 'close';
    variable?: string;
    tempFile?: DotCMSTempFile;
}

/**
 * {@link ImageEditorLauncher} for the new Angular editor.
 *
 * Opens the legacy image editor (`image-editor-standalone.jsp`) inside a PrimeNG
 * dialog via {@link DotLegacyImageEditorDialogComponent} and resolves with the
 * edited temp file forwarded by the JSP through `window.postMessage`.
 *
 * Unlike {@link LegacyDojoImageEditorLauncher} (used by the `dotcms-binary-field`
 * web component inside the Dojo `edit_field.jsp`), the new editor has no global
 * `binaryField-open-image-editor` listener, so it embeds the editor itself.
 */
@Injectable()
export class LegacyDialogImageEditorLauncher implements ImageEditorLauncher {
    readonly #dialogService = inject(DialogService);
    readonly #dotMessageService = inject(DotMessageService);

    open({ inode, tempId, variable }: ImageEditorOpenParams): Observable<DotCMSTempFile> {
        return new Observable<DotCMSTempFile>((subscriber) => {
            const data: DotLegacyImageEditorDialogData = { inode, tempId, variable };

            const dialogRef = this.#dialogService.open(DotLegacyImageEditorDialogComponent, {
                header: this.#dotMessageService.get('image-editor'),
                appendTo: 'body',
                closable: true,
                closeOnEscape: true,
                draggable: false,
                keepInViewport: false,
                maskStyleClass: 'p-dialog-mask-dynamic',
                modal: true,
                resizable: false,
                width: '90%',
                height: '90%',
                style: { 'max-width': '1200px' },
                contentStyle: { padding: '0', height: '100%', overflow: 'hidden' },
                data
            });

            const onMessage = (event: MessageEvent<ImageEditorMessage>) => {
                if (event.origin !== window.location.origin) {
                    return;
                }

                const payload = event.data;

                if (
                    !payload ||
                    payload.source !== 'dot-image-editor' ||
                    payload.variable !== variable
                ) {
                    return;
                }

                if (payload.type === 'tempfile' && payload.tempFile) {
                    subscriber.next(payload.tempFile);
                }

                if (payload.type === 'tempfile' || payload.type === 'close') {
                    dialogRef.close();
                }
            };

            window.addEventListener('message', onMessage);

            const closeSub = dialogRef.onClose.subscribe(() => subscriber.complete());

            return () => {
                window.removeEventListener('message', onMessage);
                closeSub.unsubscribe();
                dialogRef.close();
            };
        });
    }
}
