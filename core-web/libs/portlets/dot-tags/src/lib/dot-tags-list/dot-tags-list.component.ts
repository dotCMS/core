import { ChangeDetectionStrategy, Component, inject, OnDestroy } from '@angular/core';
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

import { DotTagsListStore } from './store/dot-tags-list.store';

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
        ToolbarModule
    ],
    templateUrl: './dot-tags-list.component.html',
    providers: [DotTagsListStore, DialogService, ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotTagsListComponent implements OnDestroy {
    readonly store = inject(DotTagsListStore);

    private searchTimeout: ReturnType<typeof setTimeout> | null = null;

    ngOnDestroy(): void {
        if (this.searchTimeout) {
            clearTimeout(this.searchTimeout);
        }
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
}
