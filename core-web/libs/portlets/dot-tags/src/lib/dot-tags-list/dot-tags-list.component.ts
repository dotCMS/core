import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';

import { take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotTag } from '@dotcms/dotcms-models';
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
        InputTextModule,
        IconFieldModule,
        InputIconModule,
        ConfirmDialogModule,
        ToolbarModule,
        DotMessagePipe
    ],
    templateUrl: './dot-tags-list.component.html',
    providers: [DotTagsListStore, DialogService, ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotTagsListComponent {
    readonly store = inject(DotTagsListStore);

    private readonly dialogService = inject(DialogService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly destroyRef = inject(DestroyRef);

    private searchTimeout: ReturnType<typeof setTimeout> | null = null;

    constructor() {
        this.destroyRef.onDestroy(() => {
            if (this.searchTimeout) {
                clearTimeout(this.searchTimeout);
            }
        });
    }

    onSearch(value: string): void {
        if (this.searchTimeout) {
            clearTimeout(this.searchTimeout);
        }

        this.searchTimeout = setTimeout(() => {
            this.store.setFilter(value);
        }, 300);
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
            width: '400px'
        });

        ref?.onClose.pipe(take(1)).subscribe((result) => {
            if (result) {
                this.store.createTag(result);
            }
        });
    }

    openEditDialog(tag: DotTag): void {
        const ref = this.dialogService.open(DotTagsCreateComponent, {
            header: this.dotMessageService.get('tags.edit.tag'),
            width: '400px',
            data: { tag }
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
            acceptButtonStyleClass: 'p-button-outlined',
            rejectButtonStyleClass: 'p-button-primary',
            accept: () => this.store.deleteTags()
        });
    }

    openImportDialog(): void {
        const ref = this.dialogService.open(DotTagsImportComponent, {
            header: this.dotMessageService.get('tags.import.header'),
            width: '500px'
        });

        ref?.onClose.pipe(take(1)).subscribe((result) => {
            if (result) {
                this.store.loadTags();
            }
        });
    }
}
