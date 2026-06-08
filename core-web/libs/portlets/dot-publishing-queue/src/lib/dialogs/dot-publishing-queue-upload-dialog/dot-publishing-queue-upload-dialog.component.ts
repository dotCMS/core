import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { FileSelectEvent, FileUploadModule } from 'primeng/fileupload';
import { MessageModule } from 'primeng/message';
import { ProgressBarModule } from 'primeng/progressbar';

import { DotMessagePipe } from '@dotcms/ui';

import { DotPublishingQueueStore } from '../../dot-publishing-queue-page/store/dot-publishing-queue.store';

@Component({
    selector: 'dot-publishing-queue-upload-dialog',
    standalone: true,
    imports: [ButtonModule, FileUploadModule, MessageModule, ProgressBarModule, DotMessagePipe],
    templateUrl: './dot-publishing-queue-upload-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPublishingQueueUploadDialogComponent {
    readonly store = inject(DotPublishingQueueStore);
    readonly dialogRef = inject(DynamicDialogRef);

    readonly selectedFile = signal<File | null>(null);

    onSelect(event: FileSelectEvent): void {
        const file = event.files?.[0] ?? null;
        this.selectedFile.set(file);
    }

    onClear(): void {
        this.selectedFile.set(null);
    }

    onSubmit(): void {
        const file = this.selectedFile();
        if (!file) {
            return;
        }
        this.store.uploadBundle(file, () => this.dialogRef.close({ uploaded: true }));
    }

    onCancel(): void {
        this.dialogRef.close();
    }
}
