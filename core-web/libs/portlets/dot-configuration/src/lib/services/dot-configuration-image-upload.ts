import { EMPTY } from 'rxjs';

import { DestroyRef, Signal, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { FileSelectEvent, FileUpload } from 'primeng/fileupload';

import { catchError, finalize } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';

import { DotConfigurationAssetService } from './dot-configuration-asset.service';

export interface DotConfigurationImageUpload {
    /** True while a picked file is being uploaded and published. */
    readonly $uploading: Signal<boolean>;
    /**
     * Uploads the file picked in a `p-fileupload` and hands over its `/dA/...` path. The picker
     * is cleared so the same file can be picked again; a failed upload is reported to the user
     * and `onUploaded` is not called.
     *
     * @param event - Selection event of the picker.
     * @param picker - The picker that emitted it.
     * @param onUploaded - Receives the path of the published image.
     */
    upload(
        event: FileSelectEvent,
        picker: FileUpload | undefined,
        onUploaded: (path: string) => void
    ): void;
}

/**
 * Image upload shared by the Configuration dialogs (login background, login logo, navbar logo).
 * Must run in an injection context, such as a component field initializer; an upload still
 * running when the component is destroyed is dropped.
 *
 * @returns The uploading state and the function that uploads a picked file.
 */
export function injectImageUpload(): DotConfigurationImageUpload {
    const assetService = inject(DotConfigurationAssetService);
    const httpErrorManager = inject(DotHttpErrorManagerService);
    const destroyRef = inject(DestroyRef);
    const $uploading = signal(false);

    return {
        $uploading: $uploading.asReadonly(),
        upload(event, picker, onUploaded) {
            const [file] = event.currentFiles;
            picker?.clear();

            if (!file) {
                return;
            }

            $uploading.set(true);
            assetService
                .uploadImage(file)
                .pipe(
                    catchError((error) => {
                        httpErrorManager.handle(error);

                        return EMPTY;
                    }),
                    finalize(() => $uploading.set(false)),
                    takeUntilDestroyed(destroyRef)
                )
                .subscribe(onUploaded);
        }
    };
}
