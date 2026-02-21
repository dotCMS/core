import { EMPTY } from 'rxjs';

import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FileSelectEvent, FileUploadModule } from 'primeng/fileupload';
import { RadioButton } from 'primeng/radiobutton';

import { catchError, take } from 'rxjs/operators';

import { DotCategoriesService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-categories-import',
    standalone: true,
    imports: [FileUploadModule, ButtonModule, RadioButton, FormsModule, DotMessagePipe],
    templateUrl: './dot-categories-import.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCategoriesImportComponent {
    readonly #ref = inject(DynamicDialogRef);
    readonly #config = inject(DynamicDialogConfig);
    readonly #categoriesService = inject(DotCategoriesService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);

    selectedFile = signal<File | null>(null);
    importing = signal(false);
    importType: 'replace' | 'merge' = 'merge';

    onFileSelect(event: FileSelectEvent): void {
        this.selectedFile.set(event.files?.[0] ?? null);
    }

    onFileClear(): void {
        this.selectedFile.set(null);
    }

    importFile(): void {
        const file = this.selectedFile();
        if (!file) {
            return;
        }

        this.importing.set(true);
        this.#categoriesService
            .importCategories(file, this.importType, this.#config.data?.parentInode)
            .pipe(
                take(1),
                catchError((error) => {
                    this.#httpErrorManager.handle(error);
                    this.importing.set(false);

                    return EMPTY;
                })
            )
            .subscribe(() => {
                this.importing.set(false);
                this.#ref.close(true);
            });
    }

    close(): void {
        this.#ref.close(false);
    }
}
