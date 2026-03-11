import { EMPTY } from 'rxjs';

import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { FileSelectEvent, FileUploadModule } from 'primeng/fileupload';
import { MessageModule } from 'primeng/message';
import { TooltipModule } from 'primeng/tooltip';

import { catchError, take } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotMessageService, DotTagsService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';
import { DotMessagePipe } from '@dotcms/ui';
import { getDownloadLink } from '@dotcms/utils';

@Component({
    selector: 'dot-tags-import',
    standalone: true,
    imports: [FileUploadModule, ButtonModule, MessageModule, TooltipModule, DotMessagePipe],
    templateUrl: './dot-tags-import.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotTagsImportComponent {
    readonly #ref = inject(DynamicDialogRef);
    readonly #tagsService = inject(DotTagsService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #globalStore = inject(GlobalStore);

    selectedFile = signal<File | null>(null);
    importing = signal(false);
    result = signal<{ totalRows: number; successCount: number; failureCount: number } | null>(null);
    tooltipMessage = computed(() => this.#dotMessageService.get('tags.import.tooltip'));

    resultMessage = computed(() => {
        const res = this.result();
        if (!res) return '';
        if (res.failureCount === 0) {
            return this.#dotMessageService.get('tags.import.success', `${res.successCount}`);
        }

        return (
            this.#dotMessageService.get(
                'tags.import.partial-success',
                `${res.successCount}`,
                `${res.totalRows}`
            ) +
            ' ' +
            this.#dotMessageService.get('tags.import.failures', `${res.failureCount}`)
        );
    });

    onFileSelect(event: FileSelectEvent): void {
        const file = event.files?.[0] ?? null;
        this.selectedFile.set(this.isCsvFile(file) ? file : null);
        this.result.set(null);
    }

    onFileClear(): void {
        this.selectedFile.set(null);
        this.result.set(null);
    }

    importFile(): void {
        const file = this.selectedFile();
        if (!file) {
            return;
        }

        this.importing.set(true);
        this.#tagsService
            .importTags(file)
            .pipe(
                take(1),
                catchError((error) => {
                    this.#httpErrorManager.handle(error);
                    this.importing.set(false);

                    return EMPTY;
                })
            )
            .subscribe((response) => {
                this.importing.set(false);
                this.result.set(response.entity);
            });
    }

    close(imported: boolean): void {
        this.#ref.close(imported);
    }

    downloadTemplate(): void {
        const currentSiteId = this.#globalStore.currentSiteId() ?? 'SYSTEM_HOST';
        const content =
            `"Tag Name","Host ID"\n` +
            `"Marketing","${this.escapeCsvValue(currentSiteId)}"\n` +
            `"News","${this.escapeCsvValue(currentSiteId)}"\n`;
        const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' });
        getDownloadLink(blob, 'tags-import-template.csv').click();
    }

    private isCsvFile(file: File | null): file is File {
        if (!file) {
            return false;
        }

        return file.name.toLowerCase().endsWith('.csv') || file.type === 'text/csv';
    }

    private escapeCsvValue(value: string): string {
        return value.replace(/"/g, '""');
    }
}
