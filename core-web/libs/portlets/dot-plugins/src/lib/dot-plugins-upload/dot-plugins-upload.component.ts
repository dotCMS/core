import { EMPTY } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { FileUploadModule, FileSelectEvent } from 'primeng/fileupload';
import { MessageModule } from 'primeng/message';

import { catchError, take } from 'rxjs/operators';

import { DotMessageService, DotOsgiService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-plugins-upload',
    standalone: true,
    imports: [FileUploadModule, ButtonModule, DotMessagePipe, MessageModule],
    templateUrl: './dot-plugins-upload.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPluginsUploadComponent {
    readonly #ref = inject(DynamicDialogRef);
    readonly #osgiService = inject(DotOsgiService);
    readonly #dotMessageService = inject(DotMessageService);

    selectedFiles = signal<File[]>([]);
    uploading = signal(false);
    errorMessage = signal<string | null>(null);

    /** Summary line for selected files (e.g. "file.jar" or "file.jar and 2 more"). */
    selectedFilesSummary = computed(() => {
        const files = this.selectedFiles();
        if (files.length === 0) return '';
        if (files.length === 1) return files[0].name;
        return `${files[0].name} ${this.#dotMessageService.get('plugins.upload.and-n-more', String(files.length - 1))}`;
    });

    onFileSelect(event: FileSelectEvent): void {
        const jars = event.currentFiles.filter((f) => f?.name?.toLowerCase().endsWith('.jar'));
        this.selectedFiles.set(jars);
        this.errorMessage.set(null);
    }

    onFileClear(): void {
        this.selectedFiles.set([]);
        this.errorMessage.set(null);
    }

    upload(): void {
        const files = this.selectedFiles();
        if (files.length === 0) return;

        this.uploading.set(true);
        this.errorMessage.set(null);
        this.#osgiService
            .uploadBundles(files)
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
                this.#ref.close(true);
            });
    }

    close(): void {
        this.#ref.close(null);
    }

    #extractErrorMessage(error: HttpErrorResponse): string {
        const body = error.error;

        if (Array.isArray(body) && body.length > 0) {
            return body[0]?.message || body[0]?.error || error.message;
        }

        if (body?.errors?.length > 0) {
            return body.errors[0]?.message || body.errors[0]?.error || error.message;
        }

        return body?.message || (typeof body === 'string' ? body : null) || error.message;
    }
}
