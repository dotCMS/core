import { EMPTY } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    signal,
    viewChild
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { FileUpload, FileUploadModule, FileSelectEvent } from 'primeng/fileupload';

import { catchError, take } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotMessageService, DotOsgiService } from '@dotcms/data-access';
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
    readonly #dotMessageService = inject(DotMessageService);

    readonly fileUploadRef = viewChild<FileUpload>('fileUpload');

    selectedFiles = signal<File[]>([]);
    uploading = signal(false);

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
                this.clearAndClose(true);
            });
    }

    close(): void {
        this.clearAndClose(false);
    }

    private clearAndClose(result: boolean): void {
        this.selectedFiles.set([]);
        this.fileUploadRef()?.clear();
        this.#ref.close(result);
    }
}
