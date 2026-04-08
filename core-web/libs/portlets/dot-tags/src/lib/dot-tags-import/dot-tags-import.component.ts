import { EMPTY } from 'rxjs';

import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { FileSelectEvent, FileUploadModule } from 'primeng/fileupload';
import { TooltipModule } from 'primeng/tooltip';

import { catchError, take } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotMessageService, DotTagsService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';
import { DotMessagePipe } from '@dotcms/ui';
import { getDownloadLink } from '@dotcms/utils';

@Component({
    selector: 'dot-tags-import',
    standalone: true,
    imports: [FileUploadModule, ButtonModule, TooltipModule, DotMessagePipe],
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
    tooltipMessage = computed(() => this.#dotMessageService.get('tags.import.tooltip'));

    onFileSelect(event: FileSelectEvent): void {
        const file = event.files?.[0] ?? null;
        this.selectedFile.set(this.isCsvFile(file) ? file : null);
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
                this.#ref.close(response.entity);
            });
    }

    close(): void {
        this.#ref.close(null);
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
