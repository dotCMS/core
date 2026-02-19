import { EMPTY } from 'rxjs';

import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { FileSelectEvent, FileUploadModule } from 'primeng/fileupload';

import { catchError, take } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotOsgiService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-plugins-upload',
    standalone: true,
    imports: [FileUploadModule, ButtonModule, DotMessagePipe],
    templateUrl: './dot-plugins-upload.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPluginsUploadComponent {
    readonly #ref = inject(DynamicDialogRef);
    readonly #osgiService = inject(DotOsgiService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);

    selectedFiles = signal<File[]>([]);
    uploading = signal(false);

    onFileSelect(event: FileSelectEvent): void {
        const files = event.files ?? [];
        const jars = files.filter((f) => f.name.toLowerCase().endsWith('.jar'));
        this.selectedFiles.set(jars);
    }

    onFileClear(): void {
        this.selectedFiles.set([]);
    }

    upload(): void {
        const files = this.selectedFiles();
        if (files.length === 0) return;
        this.uploading.set(true);
        this.#osgiService
            .uploadBundles(files)
            .pipe(
                take(1),
                catchError((error) => {
                    this.#httpErrorManager.handle(error);
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
        this.#ref.close(false);
    }
}
