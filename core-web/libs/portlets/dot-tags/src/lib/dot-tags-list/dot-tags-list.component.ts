import { Subject } from 'rxjs';

import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ConfirmationService, MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { SplitButtonModule } from 'primeng/splitbutton';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';

import { debounceTime, distinctUntilChanged, take } from 'rxjs/operators';

import { DotMessageDisplayService, DotMessageService } from '@dotcms/data-access';
import {
    DotMessageSeverity,
    DotMessageType,
    DotTag,
    DotTagsCreateResult,
    DotTagsImportResult
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotTagsListStore } from './store/dot-tags-list.store';

import { DotTagsCreateComponent } from '../dot-tags-create/dot-tags-create.component';
import { DotTagsImportComponent } from '../dot-tags-import/dot-tags-import.component';

@Component({
    selector: 'dot-tags-list',
    standalone: true,
    imports: [
        FormsModule,
        TableModule,
        ButtonModule,
        CheckboxModule,
        InputTextModule,
        IconFieldModule,
        InputIconModule,
        ConfirmDialogModule,
        SkeletonModule,
        SplitButtonModule,
        ToolbarModule,
        DotMessagePipe
    ],
    templateUrl: './dot-tags-list.component.html',
    providers: [DotTagsListStore, DialogService, ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0' }
})
export class DotTagsListComponent {
    readonly store = inject(DotTagsListStore);

    /** Pass-through config so the table fills 100% height when empty (empty/loading state). */
    readonly $ptConfig = computed(() => ({
        table: {
            style: {
                'table-layout': 'fixed' as const,
                ...(this.store.tags().length === 0 && {
                    height: '100%',
                    width: '100%'
                })
            }
        }
    }));

    private readonly dialogService = inject(DialogService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly dotMessageDisplayService = inject(DotMessageDisplayService);
    private readonly destroyRef = inject(DestroyRef);

    private searchSubject = new Subject<string>();

    readonly addTagMenuItems: MenuItem[] = [
        {
            label: this.dotMessageService.get('tags.import'),
            command: () => this.openImportDialog()
        }
    ];

    /**
     * Menu items for the Export split-button. "Export All" is disabled when only
     * one page of results exists (the current selection already covers everything),
     * but kept in the menu for discoverability.
     */
    readonly $exportMenuItems = computed((): MenuItem[] => [
        {
            label: this.dotMessageService.get('tags.export.selected'),
            command: () => this.store.exportSelected()
        },
        {
            label: this.dotMessageService.get('tags.export.all'),
            disabled: !this.store.showExportAll(),
            command: () => this.store.exportAll()
        }
    ]);

    constructor() {
        this.searchSubject
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
            .subscribe((value) => this.store.setFilter(value));
    }

    onSearch(value: string): void {
        this.searchSubject.next(value);
    }

    onLazyLoad(event: TableLazyLoadEvent): void {
        const rows = (event.rows as number) ?? this.store.rows();
        const first = (event.first as number) ?? 0;
        const page = Math.floor(first / rows) + 1;

        this.store.setPagination(page, rows);

        if (event.sortField) {
            const field = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;
            const order = event.sortOrder === -1 ? 'DESC' : 'ASC';
            this.store.setSort(field, order);
        }
    }

    openCreateDialog(): void {
        const ref = this.dialogService.open(DotTagsCreateComponent, {
            header: this.dotMessageService.get('tags.add.tag'),
            width: '700px',
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });

        ref?.onClose.pipe(take(1)).subscribe((result) => {
            if (result) {
                this.store.createTag(result, (created) => this.notifyCreateResult(created));
            }
        });
    }

    openEditDialog(tag: DotTag): void {
        const ref = this.dialogService.open(DotTagsCreateComponent, {
            header: this.dotMessageService.get('tags.edit.tag'),
            width: '700px',
            data: { tag },
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });

        ref?.onClose.pipe(take(1)).subscribe((result) => {
            if (result) {
                this.store.updateTag(tag, result);
            }
        });
    }

    confirmDelete(): void {
        const count = this.store.selectedTags().length;

        this.confirmationService.confirm({
            message: this.dotMessageService.get('tags.confirm.delete.message', `${count}`),
            header: this.dotMessageService.get('tags.confirm.delete.header'),
            acceptLabel: this.dotMessageService.get('tags.delete'),
            rejectLabel: this.dotMessageService.get('tags.cancel'),
            acceptButtonStyleClass: 'p-button-primary',
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            position: 'center',
            accept: () => this.store.deleteTags()
        });
    }

    openImportDialog(): void {
        const ref = this.dialogService.open(DotTagsImportComponent, {
            header: this.dotMessageService.get('tags.import.header'),
            width: '700px',
            contentStyle: { height: '460px' },
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });

        ref?.onClose.pipe(take(1)).subscribe((result: DotTagsImportResult | null) => {
            if (result) {
                this.store.loadTags();
                this.notifyImportResult(result);
            }
        });
    }

    /**
     * Tells the user what a create request did. The backend answers 200 even when the tag
     * already existed, so without this a duplicate submit looks like a silent no-op.
     */
    private notifyCreateResult({ created, duplicates }: DotTagsCreateResult): void {
        if (created.length === 0 && duplicates.length === 0) {
            return;
        }

        let severity = DotMessageSeverity.INFO;
        let message: string;

        if (duplicates.length === 0) {
            severity = DotMessageSeverity.SUCCESS;
            message = this.dotMessageService.get('tags.create.success', `${created.length}`);
        } else if (created.length === 0) {
            message = this.dotMessageService.get(
                'tags.create.duplicate',
                duplicates.map((tag) => tag.label).join(', ')
            );
        } else {
            message = this.dotMessageService.get(
                'tags.create.mixed',
                `${created.length}`,
                `${duplicates.length}`
            );
        }

        this.dotMessageDisplayService.push({
            life: 5000,
            severity,
            message,
            type: DotMessageType.SIMPLE_MESSAGE
        });
    }

    /**
     * Shows the import outcome. If any row was skipped as a duplicate (and none failed)
     * the toast is INFO, even when other rows were created, so skipped tags are never
     * reported as a plain success. The total is the sum of the counts because the
     * backend's `totalRows` includes the CSV header line.
     */
    private notifyImportResult({
        successCount = 0,
        duplicateCount = 0,
        failureCount = 0
    }: DotTagsImportResult): void {
        const total = `${successCount + duplicateCount + failureCount}`;
        let severity = DotMessageSeverity.SUCCESS;
        let message = this.dotMessageService.get('tags.import.success', `${successCount}`);

        if (failureCount > 0) {
            severity = DotMessageSeverity.WARNING;
            message = this.dotMessageService.get(
                'tags.import.partial-success',
                `${successCount}`,
                total,
                `${failureCount}`,
                `${duplicateCount}`
            );
        } else if (duplicateCount > 0) {
            severity = DotMessageSeverity.INFO;
            message = this.dotMessageService.get(
                'tags.import.with-duplicates',
                `${successCount}`,
                `${duplicateCount}`,
                total
            );
        }

        this.dotMessageDisplayService.push({
            life: 5000,
            severity,
            message,
            type: DotMessageType.SIMPLE_MESSAGE
        });
    }
}
