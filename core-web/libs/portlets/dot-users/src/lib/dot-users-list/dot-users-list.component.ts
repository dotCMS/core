import { Subject } from 'rxjs';

import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { MenuItem } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DialogService } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { Menu, MenuModule } from 'primeng/menu';
import { SkeletonModule } from 'primeng/skeleton';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';

import { debounceTime, distinctUntilChanged, take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { DotEnvironment } from '@dotcms/dotcms-models';
import { DotAddToBundleComponent, DotMessagePipe } from '@dotcms/ui';

import { DotUsersFilterByComponent } from './components/dot-users-filter-by/dot-users-filter-by.component';
import { DotUsersListStore } from './store/dot-users-list.store';

import { DotUsersReplacementPickerComponent } from '../components/dot-users-replacement-picker/dot-users-replacement-picker.component';
import {
    DotUsersCreateComponent,
    DotUsersDialogResult
} from '../dot-users-create/dot-users-create.component';
import { DotUserListItem } from '../services/dot-users.service';

/**
 * Legacy backend contract: Push Publish and Add to Bundle identify a
 * user asset by the prefix `user_` on the raw userId. `users_` (empty
 * suffix) means "the current selection", which the pushHandler backend
 * resolves against the session — matching what the legacy Dojo portlet
 * did in `view_users_js_inc.jsp`. Bulk selections stringify with the
 * `user_` prefix on each id, comma-joined, mirroring how content-drive
 * hands multiple identifiers to the same dialog.
 */
const USER_ASSET_PREFIX = 'user_';

@Component({
    selector: 'dot-users-list',
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
        MenuModule,
        SkeletonModule,
        TagModule,
        ToolbarModule,
        DotAddToBundleComponent,
        DotMessagePipe,
        DotUsersFilterByComponent,
        DotUsersReplacementPickerComponent
    ],
    templateUrl: './dot-users-list.component.html',
    styleUrl: './dot-users-list.component.scss',
    providers: [DotUsersListStore, DialogService],
    host: { class: 'flex flex-col h-full min-h-0' }
})
export class DotUsersListComponent {
    readonly store = inject(DotUsersListStore);

    readonly #dialogService = inject(DialogService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #pushPublishDialogService = inject(DotPushPublishDialogService);
    readonly #destroyRef = inject(DestroyRef);
    readonly #route = inject(ActivatedRoute);

    readonly #searchSubject = new Subject<string>();

    protected readonly $bulkDeleteVisible = signal(false);
    protected readonly $bulkReplacementUser = signal<DotUserListItem | null>(null);
    /**
     * Flips on when Delete is clicked with an invalid form. Drives the
     * footer validation hint; the button stays enabled per design.
     */
    protected readonly $bulkDeleteAttempted = signal(false);

    /**
     * Identifier fed to `<dot-add-to-bundle>`. Null closes the dialog;
     * a string opens it. Same signal shape used by dot-plugins and
     * dot-experiments so the component's `(cancel)` callback resets it.
     */
    protected readonly $addToBundleAssetId = signal<string | null>(null);

    /**
     * Resolver output from the users route. Push Publish and Add to
     * Bundle only make sense when the license permits and at least one
     * environment is registered — otherwise the row menu drops those
     * two entries entirely (dot-locales convention).
     */
    readonly #envs = (this.#route.snapshot.data['pushPublishEnvironments'] ??
        []) as DotEnvironment[];
    readonly #isEnterprise = !!this.#route.snapshot.data['isEnterprise'];
    protected readonly $isPushPublishEnabled = signal(this.#isEnterprise && this.#envs.length > 0);

    /**
     * The picker must never surface any user currently selected for
     * deletion — same rule as the single-delete flow but generalized
     * to the whole selection.
     */
    protected readonly $bulkExcludedIds = computed(() =>
        this.store.selectedUsers().map((user) => user.userId)
    );

    protected readonly $canConfirmBulkDelete = computed(() => {
        const replacement = this.$bulkReplacementUser();
        if (!replacement) {
            return false;
        }

        return !this.$bulkExcludedIds().includes(replacement.userId);
    });

    /**
     * Field-level error surfaced under the replacement picker after
     * the user tries to click Delete with an invalid state. Same
     * pattern as the single-delete flow.
     */
    protected readonly $bulkReplacementError = computed(() => {
        if (!this.$bulkDeleteAttempted()) {
            return null;
        }

        const replacement = this.$bulkReplacementUser();
        if (!replacement) {
            return 'users.dialog.delete-confirm.replacement.required';
        }
        if (this.$bulkExcludedIds().includes(replacement.userId)) {
            return 'users.dialog.delete-confirm.replacement.self';
        }

        return null;
    });

    /**
     * i18n key for the bulk-delete dialog's footer warning. Same rule
     * as the main dialog's `$formWarning`: single generic message on
     * the footer row, per-field inline errors stay under each field.
     */
    protected readonly $bulkDeleteWarning = computed(() =>
        this.$bulkReplacementError() ? 'users.dialog.warning.form-errors' : null
    );

    /**
     * Menu items for the trailing kebab on a single row. Scope is
     * intentionally narrow: Edit is already the row's own click target,
     * Delete is the selection-toolbar action, so the kebab only holds
     * the two publish-adjacent commands. Empty on non-enterprise
     * instances — the template skips rendering the kebab entirely in
     * that case so users don't see a dead trigger.
     */
    protected getRowMenuItems(user: DotUserListItem): MenuItem[] {
        if (!this.$isPushPublishEnabled()) {
            return [];
        }

        return [
            {
                label: this.#dotMessageService.get('users.actions.push-publish'),
                command: () => this.openRowPushPublish(user)
            },
            {
                label: this.#dotMessageService.get('users.actions.add-to-bundle'),
                command: () => this.openRowAddToBundle(user)
            }
        ];
    }

    constructor() {
        this.#searchSubject
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.#destroyRef))
            .subscribe((value) => this.store.setFilter(value));
    }

    onSearch(value: string): void {
        this.#searchSubject.next(value);
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
        // Edit mode: show the user's name in the dialog title so the
        // internal avatar/name header row can drop. Fall back through
        // fullName → name → email so a record with a missing display
        // name still gets a stable title (matches what the list column
        // renders).
        const editHeader =
            user &&
            (user.fullName?.trim() ||
                user.name?.trim() ||
                user.emailAddress?.trim() ||
                this.#dotMessageService.get('users.edit.header'));
        const ref = this.#dialogService.open(DotUsersCreateComponent, {
            header: user ? editHeader : this.#dotMessageService.get('users.create.header'),
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
                    this.store.createUser({
                        payload: result.payload,
                        gettingStartedChange: result.gettingStartedChange
                    });
                } else {
                    this.store.updateUser({
                        payload: result.payload,
                        gettingStartedChange: result.gettingStartedChange
                    });
                }
            } else if (result.action === 'delete') {
                this.store.deleteSingleUser({
                    userId: result.userId,
                    replacementUserId: result.replacementUserId
                });
            }
        });
    }

    confirmDelete(): void {
        this.$bulkReplacementUser.set(null);
        this.$bulkDeleteAttempted.set(false);
        this.$bulkDeleteVisible.set(true);
    }

    closeBulkDelete(): void {
        this.$bulkDeleteVisible.set(false);
    }

    onBulkReplacementSelect(user: DotUserListItem | null): void {
        this.$bulkReplacementUser.set(user);
        this.$bulkDeleteAttempted.set(false);
    }

    confirmBulkDelete(): void {
        const replacement = this.$bulkReplacementUser();
        if (!this.$canConfirmBulkDelete() || !replacement) {
            this.$bulkDeleteAttempted.set(true);

            return;
        }

        this.$bulkDeleteVisible.set(false);
        this.store.deleteSelectedUsers(replacement.userId);
    }

    /** Row kebab → Push to Publish for a single user. */
    openRowPushPublish(user: DotUserListItem): void {
        this.#pushPublishDialogService.open({
            assetIdentifier: `${USER_ASSET_PREFIX}${user.userId}`,
            title: this.#dotMessageService.get('contenttypes.content.push_publish')
        });
    }

    /** Row kebab → Add to Bundle for a single user. */
    openRowAddToBundle(user: DotUserListItem): void {
        this.$addToBundleAssetId.set(`${USER_ASSET_PREFIX}${user.userId}`);
    }

    /**
     * Selection toolbar → Push to Publish for every selected user.
     * Sends one comma-joined identifier the way content-drive does; the
     * backend's RemotePublishAjaxAction splits on commas.
     */
    openBulkPushPublish(): void {
        const ids = this.#joinSelectionAssetIds();
        if (!ids) {
            return;
        }

        this.#pushPublishDialogService.open({
            assetIdentifier: ids,
            title: this.#dotMessageService.get('contenttypes.content.push_publish')
        });
    }

    /** Selection toolbar → Add to Bundle for every selected user. */
    openBulkAddToBundle(): void {
        const ids = this.#joinSelectionAssetIds();
        if (!ids) {
            return;
        }

        this.$addToBundleAssetId.set(ids);
    }

    #joinSelectionAssetIds(): string | null {
        const selected = this.store.selectedUsers();
        if (selected.length === 0) {
            return null;
        }

        return selected.map((user) => `${USER_ASSET_PREFIX}${user.userId}`).join(',');
    }

    /**
     * Opens the row kebab. Passes the click event straight to the menu
     * so PrimeNG positions the popup on the trigger regardless of how
     * many rows the table renders — no per-row viewChild needed.
     */
    openRowMenu(event: Event, menu: Menu): void {
        event.stopPropagation();
        menu.toggle(event);
    }

    /**
     * Formatted Roles-column data keyed by userId. Derived once per
     * `store.userRoles()` change instead of allocating a fresh object
     * from a template-called method on every row per CD pass. The
     * template does a plain map lookup: `$rolesByUserId()[user.userId]`.
     *
     * Entries are `null` while the per-user role fetch is still in
     * flight so the cell renders empty instead of a misleading
     * `and -2 more`.
     */
    protected readonly $rolesByUserId = computed<
        Record<string, { visible: string; more: number } | null>
    >(() => {
        const source = this.store.userRoles();

        return Object.fromEntries(
            Object.entries(source).map(([userId, roles]) => {
                if (!roles || roles.length === 0) {
                    return [userId, null];
                }

                return [
                    userId,
                    {
                        visible: roles.slice(0, 2).join(', '),
                        more: Math.max(0, roles.length - 2)
                    }
                ];
            })
        );
    });

    initials(user: DotUserListItem): string {
        const first = (user.firstName ?? '').charAt(0);
        const last = (user.lastName ?? '').charAt(0);

        return `${first}${last}`.toUpperCase() || '?';
    }
}
