import { Subject } from 'rxjs';

import { DatePipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    inject,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
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

import { DotUsersReplacementPickerComponent } from '../components/dot-users-replacement-picker/dot-users-replacement-picker.component';
import {
    DotUsersCreateComponent,
    DotUsersDialogResult
} from '../dot-users-create/dot-users-create.component';
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
        DialogModule,
        InputTextModule,
        IconFieldModule,
        InputIconModule,
        SkeletonModule,
        TagModule,
        ToolbarModule,
        DotMessagePipe,
        DotUsersFilterByComponent,
        DotUsersReplacementPickerComponent
    ],
    templateUrl: './dot-users-list.component.html',
    styleUrl: './dot-users-list.component.scss',
    providers: [DotUsersListStore, DialogService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0' }
})
export class DotUsersListComponent {
    readonly store = inject(DotUsersListStore);

    private readonly dialogService = inject(DialogService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly destroyRef = inject(DestroyRef);

    private readonly searchSubject = new Subject<string>();

    protected readonly bulkDeleteVisible = signal(false);
    protected readonly bulkReplacementUser = signal<DotUserListItem | null>(null);
    /**
     * Flips on when Delete is clicked with an invalid form. Drives the
     * footer validation hint; the button stays enabled per design.
     */
    protected readonly bulkDeleteAttempted = signal(false);

    /**
     * The picker must never surface any user currently selected for
     * deletion — same rule as the single-delete flow but generalized
     * to the whole selection.
     */
    protected readonly bulkExcludedIds = computed(() =>
        this.store.selectedUsers().map((user) => user.userId)
    );

    protected readonly canConfirmBulkDelete = computed(() => {
        const replacement = this.bulkReplacementUser();
        if (!replacement) {
            return false;
        }

        return !this.bulkExcludedIds().includes(replacement.userId);
    });

    /**
     * Field-level error surfaced under the replacement picker after
     * the user tries to click Delete with an invalid state. Same
     * pattern as the single-delete flow.
     */
    protected readonly bulkReplacementError = computed(() => {
        if (!this.bulkDeleteAttempted()) {
            return null;
        }

        const replacement = this.bulkReplacementUser();
        if (!replacement) {
            return 'users.dialog.delete-confirm.replacement.required';
        }
        if (this.bulkExcludedIds().includes(replacement.userId)) {
            return 'users.dialog.delete-confirm.replacement.self';
        }

        return null;
    });

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

        let sortField = this.store.sortField();
        let sortOrder: 'ASC' | 'DESC' = this.store.sortOrder();
        if (event.sortField) {
            sortField = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;
            sortOrder = event.sortOrder === -1 ? 'DESC' : 'ASC';
        }

        this.store.applyLazyLoad({ page, rows, sortField, sortOrder });
    }

    openCreateDialog(): void {
        this.openUserDialog();
    }

    openEditDialog(user: DotUserListItem): void {
        this.openUserDialog(user);
    }

    /**
     * The create/edit dialog hosts four tabs (Profile, Roles,
     * Permissions, API Tokens) plus a legacy JSP iframe for
     * Permissions, so it opens at the "Special" width bucket
     * documented in `libs/portlets/CLAUDE.md` — much wider than the
     * standard 700px form dialog.
     */
    private openUserDialog(user?: DotUserListItem): void {
        const ref = this.dialogService.open(DotUsersCreateComponent, {
            header: this.dotMessageService.get(user ? 'users.edit.header' : 'users.create.header'),
            width: 'min(92vw, 75rem)',
            height: 'min(90vh, 48rem)',
            data: user ? { user } : undefined,
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center',
            contentStyle: { padding: '0', overflow: 'hidden' },
            styleClass: 'p-dialog-content-flush'
        });

        ref?.onClose.pipe(take(1)).subscribe((result: DotUsersDialogResult | undefined) => {
            if (!result) {
                return;
            }

            if (result.action === 'save') {
                if (result.mode === 'create') {
                    this.store.createUser(result.payload, result.gettingStartedChange);
                } else {
                    this.store.updateUser(result.payload, result.gettingStartedChange);
                }
            } else if (result.action === 'delete') {
                this.store.deleteSingleUser(result.userId, result.replacementUserId);
            }
        });
    }

    confirmDelete(): void {
        this.bulkReplacementUser.set(null);
        this.bulkDeleteAttempted.set(false);
        this.bulkDeleteVisible.set(true);
    }

    closeBulkDelete(): void {
        this.bulkDeleteVisible.set(false);
    }

    onBulkReplacementSelect(user: DotUserListItem | null): void {
        this.bulkReplacementUser.set(user);
        this.bulkDeleteAttempted.set(false);
    }

    confirmBulkDelete(): void {
        const replacement = this.bulkReplacementUser();
        if (!this.canConfirmBulkDelete() || !replacement) {
            this.bulkDeleteAttempted.set(true);

            return;
        }

        this.bulkDeleteVisible.set(false);
        this.store.deleteSelectedUsers(replacement.userId);
    }

    /**
     * Formats the Roles column: first two role names comma-separated,
     * followed by `and N more` when the user carries more. Returns
     * `null` while the per-user role fetch is still in flight so the
     * cell renders empty instead of a misleading `and -2 more`.
     */
    formatRoles(userId: string): { visible: string; more: number } | null {
        const roles = this.store.userRoles()[userId];
        if (!roles || roles.length === 0) {
            return null;
        }

        return {
            visible: roles.slice(0, 2).join(', '),
            more: Math.max(0, roles.length - 2)
        };
    }

    initials(user: DotUserListItem): string {
        const first = (user.firstName ?? '').charAt(0);
        const last = (user.lastName ?? '').charAt(0);

        return `${first}${last}`.toUpperCase() || '?';
    }
}
