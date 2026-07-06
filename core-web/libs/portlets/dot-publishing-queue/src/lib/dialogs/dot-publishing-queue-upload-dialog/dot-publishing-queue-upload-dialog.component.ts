import { EMPTY } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { FileSelectEvent, FileUploadModule } from 'primeng/fileupload';
import { MessageModule } from 'primeng/message';

import { catchError, take } from 'rxjs/operators';

import { DotMessageService, DotPublishingQueueService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPublishingQueueStore } from '../../store/dot-publishing-queue.store';

/** `.tar.gz` (most common) and `.tgz` (legacy alias). */
const BUNDLE_FILE_PATTERN = /\.(tar\.gz|tgz)$/i;

/**
 * Bundle upload dialog. Follows the canonical `dot-tags-import` pattern:
 * - drag-and-drop advanced file upload (NOT `mode="basic"`)
 * - upload state + error state owned by the component (NOT the store)
 * - errors surface inline via `<p-message>` instead of a global toast — so
 *   the user can correct the file and retry without closing the modal
 *
 * Calls the service directly and triggers `store.refresh()` on success.
 */
@Component({
    selector: 'dot-publishing-queue-upload-dialog',
    imports: [ButtonModule, FileUploadModule, MessageModule, DotMessagePipe],
    templateUrl: './dot-publishing-queue-upload-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPublishingQueueUploadDialogComponent {
    readonly dialogRef = inject(DynamicDialogRef);
    readonly #service = inject(DotPublishingQueueService);
    readonly #store = inject(DotPublishingQueueStore);
    readonly #dotMessageService = inject(DotMessageService);

    readonly selectedFile = signal<File | null>(null);
    readonly uploading = signal(false);
    readonly errorMessage = signal<string | null>(null);

    onFileSelect(event: FileSelectEvent): void {
        const file = event.files?.[0] ?? null;
        if (file && !this.#isBundleFile(file)) {
            this.selectedFile.set(null);
            this.errorMessage.set(
                this.#dotMessageService.get('publishing-queue.upload.warning.invalid-file-type')
            );
            return;
        }
        this.selectedFile.set(file);
        this.errorMessage.set(null);
    }

    onFileClear(): void {
        this.selectedFile.set(null);
        this.errorMessage.set(null);
    }

    onSubmit(): void {
        const file = this.selectedFile();
        // Upload stays clickable at all times — clicking without a file surfaces
        // the file-required warning inline via `errorMessage` instead of doing
        // nothing silently. Auto-clears when the user picks a valid file
        // (see `onFileSelect`).
        if (!file) {
            this.errorMessage.set(
                this.#dotMessageService.get('publishing-queue.upload.warning.file-required')
            );
            return;
        }

        this.uploading.set(true);
        this.errorMessage.set(null);

        this.#service
            .uploadBundle(file)
            .pipe(
                take(1),
                catchError((error: HttpErrorResponse) => {
                    this.errorMessage.set(this.#extractErrorMessage(error));
                    this.uploading.set(false);
                    return EMPTY;
                })
            )
            .subscribe(() => {
                this.uploading.set(false);
                this.#store.refresh();
                this.dialogRef.close({ uploaded: true });
            });
    }

    onCancel(): void {
        this.dialogRef.close();
    }

    /** Pulls the most useful message out of a dotCMS error body. Handles the
     * four shapes the BE returns: array, `{ errors: [...] }`, `{ message: ... }`,
     * or a plain string. Uses nullish coalescing so empty-string BE fields don't
     * silently fall through to the HTTP error message. */
    #extractErrorMessage(error: HttpErrorResponse): string {
        const body = error.error;

        if (Array.isArray(body) && body.length > 0) {
            return body[0]?.message ?? body[0]?.error ?? error.message;
        }

        if (body?.errors?.length > 0) {
            return body.errors[0]?.message ?? body.errors[0]?.error ?? error.message;
        }

        if (typeof body?.message === 'string') {
            return body.message;
        }

        if (typeof body === 'string') {
            return body;
        }

        return error.message;
    }

    #isBundleFile(file: File | null): file is File {
        if (!file) {
            return false;
        }
        return BUNDLE_FILE_PATTERN.test(file.name);
    }
}
