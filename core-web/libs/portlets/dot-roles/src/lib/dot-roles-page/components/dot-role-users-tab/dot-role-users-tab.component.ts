import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ListboxModule } from 'primeng/listbox';
import { Popover, PopoverModule } from 'primeng/popover';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { map } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSResponse } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotRoleMember } from '../../../models/dot-roles.models';
import { DotRolesStore } from '../../store/dot-roles.store';

interface UserFilterResult {
    userId: string;
    firstName?: string;
    lastName?: string;
    emailAddress?: string;
}

@Component({
    selector: 'dot-role-users-tab',
    standalone: true,
    imports: [
        FormsModule,
        AvatarModule,
        ButtonModule,
        TableModule,
        TagModule,
        ListboxModule,
        PopoverModule,
        TooltipModule,
        ConfirmDialogModule,
        DotMessagePipe
    ],
    providers: [ConfirmationService],
    templateUrl: './dot-role-users-tab.component.html',
    host: { class: 'block py-4' }
})
export class DotRoleUsersTabComponent {
    protected readonly store = inject(DotRolesStore);
    readonly #http = inject(HttpClient);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #messageService = inject(DotMessageService);

    protected readonly $userSuggestions = signal<UserFilterResult[]>([]);

    protected readonly $canBulkRemove = computed(() => this.store.selectedMembers().length > 0);

    constructor() {
        // Load members when the selected role changes. Users are shown even
        // when the role has `editUsers === false` (so admins can review who
        // is inherited from ancestors); the tab still surfaces the "cannot
        // grant" banner and disables the Grant / Remove actions upstream.
        effect(() => {
            const selectedRole = this.store.selectedRole();
            if (!selectedRole) {
                return;
            }
            untracked(() =>
                this.store.loadMembers({
                    id: selectedRole.id,
                    roleKey: selectedRole.roleKey ?? null
                })
            );
        });
    }

    /**
     * Popover open hook — pre-populates the user list so the panel shows a
     * browsable list immediately (matching the design), no keystroke needed.
     * `p-listbox`'s internal `filter` handles narrowing from there.
     */
    protected onGrantPanelShow(): void {
        this.#getUserSuggestions('').subscribe((users) => {
            this.$userSuggestions.set(users);
        });
    }

    /**
     * Grant the picked user to the currently-selected role via
     * `POST /v1/roles/{roleId}/users/{userId}` (issue #36937). The BE is
     * idempotent — re-granting a role the user already holds is a no-op — so
     * we don't have to gate on client-side dedup. On success the store
     * refreshes the members list so the new row lands in the table with the
     * correct `grantedFromRoleId` labelling.
     */
    protected onGrantUser(user: UserFilterResult, panel: Popover): void {
        panel.hide();
        this.store.grantUserToRole(user.userId);
    }

    /**
     * Bulk-remove the currently-selected direct members from the role
     * (DELETE /v1/roles/{roleId}/users — issue #36938).
     *
     * The store already filters `selectedMembers` to direct grants only, so
     * we pass their ids straight through. On partial-success (some users
     * skipped because they are inherited or the id didn't match), the store
     * still prunes the removed rows and refetches — future UX may surface
     * the `skipped` list explicitly, but for now the refetch keeps the
     * table honest.
     */
    protected onRemoveSelected(): void {
        const selected = this.store.selectedMembers();
        if (selected.length === 0) {
            return;
        }

        this.#confirmationService.confirm({
            message: this.#messageService.get(
                'roles.users.confirm.remove.message',
                `${selected.length}`
            ),
            header: this.#messageService.get('roles.users.confirm.remove.header'),
            acceptLabel: this.#messageService.get('roles.users.remove'),
            rejectLabel: this.#messageService.get('roles.action.cancel'),
            acceptButtonStyleClass: 'p-button-danger',
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            position: 'center',
            accept: () => {
                const userIds = selected.map((m) => m.userId);
                this.store.removeUsersFromRole(userIds);
            }
        });
    }

    /**
     * A member row is directly granted to the currently-selected role
     * (as opposed to inherited from an ancestor) when its
     * `grantedFromRoleId` matches the selected role. Only direct rows
     * are removable from this tab.
     */
    protected isDirectGrant(member: DotRoleMember): boolean {
        return member.grantedFromRoleId === this.store.selectedRoleId();
    }

    /**
     * Compact initials for `<p-avatar label>` when no photo URL is
     * available (the users API doesn't return one today). Falls back to
     * the email's first letter, then `?` so we never render an empty avatar.
     */
    protected initialsFor(user: {
        firstName?: string;
        lastName?: string;
        emailAddress?: string;
    }): string {
        const first = user.firstName?.trim()?.[0] ?? '';
        const last = user.lastName?.trim()?.[0] ?? '';
        const initials = `${first}${last}`.toUpperCase();
        if (initials) {
            return initials;
        }

        const emailInitial = user.emailAddress?.trim()?.[0]?.toUpperCase() ?? '';

        return emailInitial || '?';
    }

    protected onSelectionChange(members: DotRoleMember[]): void {
        // Filter out inherited rows in case a user managed to check one
        // (e.g., via header checkbox). Only direct grants can be removed.
        const direct = members.filter((m) => this.isDirectGrant(m));
        this.store.setSelectedMembers(direct);
    }

    #getUserSuggestions(query: string): Observable<UserFilterResult[]> {
        const q = query ? `?query=${encodeURIComponent(query)}` : '';
        return this.#http
            .get<DotCMSResponse<UserFilterResult[]>>(`/api/v1/users/filter${q}`)
            .pipe(map((response) => response.entity ?? []));
    }
}
