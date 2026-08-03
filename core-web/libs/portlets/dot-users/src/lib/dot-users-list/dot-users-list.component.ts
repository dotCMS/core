import { Subject } from 'rxjs';

import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';

import { debounceTime, distinctUntilChanged, take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotUsersFilterByComponent } from './components/dot-users-filter-by/dot-users-filter-by.component';
import { DotUsersListStore } from './store/dot-users-list.store';

import { DotUsersCreateComponent } from '../dot-users-create/dot-users-create.component';
import { DotUserListItem } from '../services/dot-users.service';

@Component({
    selector: 'dot-users-list',
    standalone: true,
    imports: [
        DatePipe,
        FormsModule,
        TableModule,
        AvatarModule,
        ButtonModule,
        InputTextModule,
        IconFieldModule,
        InputIconModule,
        ConfirmDialogModule,
        SkeletonModule,
        TagModule,
        ToolbarModule,
        DotMessagePipe,
        DotUsersFilterByComponent
    ],
    templateUrl: './dot-users-list.component.html',
    providers: [DotUsersListStore, DialogService, ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0' }
})
export class DotUsersListComponent {
    readonly store = inject(DotUsersListStore);

    private readonly dialogService = inject(DialogService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly destroyRef = inject(DestroyRef);

    private readonly searchSubject = new Subject<string>();

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
        const ref = this.dialogService.open(DotUsersCreateComponent, {
            header: this.dotMessageService.get('users.create.header'),
            width: '700px',
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });

        ref?.onClose.pipe(take(1)).subscribe(() => {
            // Create dialog is delivered by sibling ticket #36717 (Profile tab).
            // Once wired, reload the list here on success.
        });
    }

    openEditDialog(user: DotUserListItem): void {
        const ref = this.dialogService.open(DotUsersCreateComponent, {
            header: this.dotMessageService.get('users.edit.header'),
            width: '700px',
            data: { user },
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });

        ref?.onClose.pipe(take(1)).subscribe(() => {
            // Edit dialog delivered by sibling tickets.
        });
    }

    confirmDelete(): void {
        const count = this.store.selectedUsers().length;

        this.confirmationService.confirm({
            message: this.dotMessageService.get('users.confirm.delete.message', `${count}`),
            header: this.dotMessageService.get('users.confirm.delete.header'),
            acceptLabel: this.dotMessageService.get('users.delete'),
            rejectLabel: this.dotMessageService.get('users.cancel'),
            acceptButtonStyleClass: 'p-button-danger',
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            position: 'center',
            accept: () => this.store.deleteSelectedUsers()
        });
    }

    initials(user: DotUserListItem): string {
        const first = (user.firstName ?? '').charAt(0);
        const last = (user.lastName ?? '').charAt(0);

        return `${first}${last}`.toUpperCase() || '?';
    }
}
