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

import { DotMessageService } from '@dotcms/data-access';
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
    readonly #dotMessageService = inject(DotMessageService);

    readonly fileUploadRef = viewChild<FileUpload>('fileUpload');

    selectedFiles = signal<File[]>([]);

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

    /** Closes the dialog with the selected files so the list store handles the upload. */
    upload(): void {
        const files = this.selectedFiles();
        if (files.length === 0) return;
        this.clearAndClose(files);
    }

    close(): void {
        this.clearAndClose(null);
    }

    private clearAndClose(result: File[] | null): void {
        this.selectedFiles.set([]);
        this.fileUploadRef()?.clear();
        this.#ref.close(result);
    }
}
