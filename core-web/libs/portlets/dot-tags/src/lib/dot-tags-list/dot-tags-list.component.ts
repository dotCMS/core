import { Subject } from 'rxjs';

import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ConfirmationService, MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SplitButtonModule } from 'primeng/splitbutton';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';

import { debounceTime, distinctUntilChanged, take } from 'rxjs/operators';

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
        SplitButtonModule,
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

    private searchSubject = new Subject<string>();

    readonly addTagMenuItems: MenuItem[] = [
        {
            label: this.dotMessageService.get('tags.import'),
            icon: 'pi pi-upload',
            command: () => this.openImportDialog()
        }
    ];

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
            width: '400px',
            closable: true,
            closeOnEscape: true
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
            data: { tag },
            closable: true,
            closeOnEscape: true
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
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => this.store.deleteTags()
        });
    }

    openImportDialog(): void {
        const ref = this.dialogService.open(DotTagsImportComponent, {
            header: this.dotMessageService.get('tags.import.header'),
            width: '500px',
            closable: true,
            closeOnEscape: true
        });

        ref?.onClose.pipe(take(1)).subscribe((result) => {
            if (result) {
                this.store.loadTags();
            }
        });
    }
}
