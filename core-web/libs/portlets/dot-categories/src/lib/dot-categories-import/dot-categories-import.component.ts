import { EMPTY } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FileSelectEvent, FileUpload, FileUploadModule } from 'primeng/fileupload';
import { MessageModule } from 'primeng/message';
import { RadioButton } from 'primeng/radiobutton';
import { TooltipModule } from 'primeng/tooltip';

import { catchError, take } from 'rxjs/operators';

import { DotCategoriesService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-categories-import',
    imports: [
        FileUploadModule,
        ButtonModule,
        RadioButton,
        FormsModule,
        MessageModule,
        DotMessagePipe,
        TooltipModule
    ],
    templateUrl: './dot-categories-import.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCategoriesImportComponent {
    readonly #ref = inject(DynamicDialogRef);
    readonly #config = inject(DynamicDialogConfig);
    readonly #categoriesService = inject(DotCategoriesService);
    private readonly fileUploadRef = viewChild<FileUpload>('fileUpload');

    readonly $selectedFile = signal<File | null>(null);
    readonly $importing = signal(false);
    readonly $errorMessage = signal<string | null>(null);
    importType: 'replace' | 'merge' = 'merge';

    onFileSelect(event: FileSelectEvent): void {
        this.$selectedFile.set(event.files?.[0] ?? null);
        this.$errorMessage.set(null);
    }

    onFileClear(): void {
        this.$selectedFile.set(null);
        this.$errorMessage.set(null);
        // When the custom remove button is clicked, PrimeNG's internal files array is not
        // automatically cleared. We call clear() here to sync it, but only when PrimeNG
        // still holds the file — otherwise we'd loop back through (onClear) → onFileClear().
        const uploader = this.fileUploadRef();
        if (uploader && uploader.files.length > 0) {
            uploader.clear();
        }
    }

    importFile(): void {
        const file = this.$selectedFile();
        if (!file) {
            return;
        }

        this.$importing.set(true);
        this.$errorMessage.set(null);
        this.#categoriesService
            .importCategories(file, this.importType, this.#config.data?.parentInode)
            .pipe(
                take(1),
                catchError((error: HttpErrorResponse) => {
                    this.$errorMessage.set(this.#extractErrorMessage(error));
                    this.$importing.set(false);

                    return EMPTY;
                })
            )
            .subscribe((response) => {
                this.$importing.set(false);
                this.#ref.close(response.entity);
            });
    }

    close(): void {
        this.#ref.close(false);
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
