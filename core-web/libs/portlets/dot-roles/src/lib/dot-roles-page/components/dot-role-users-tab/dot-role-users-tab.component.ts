import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AutoCompleteCompleteEvent, AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { Popover, PopoverModule } from 'primeng/popover';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { map } from 'rxjs/operators';

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
        ButtonModule,
        TableModule,
        TagModule,
        AutoCompleteModule,
        PopoverModule,
        TooltipModule,
        DotMessagePipe
    ],
    templateUrl: './dot-role-users-tab.component.html',
    host: { class: 'block py-4' }
})
export class DotRoleUsersTabComponent {
    protected readonly store = inject(DotRolesStore);
    readonly #http = inject(HttpClient);

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

    protected searchUsers(event: AutoCompleteCompleteEvent): void {
        const query = event.query?.trim();
        this.#getUserSuggestions(query ?? '').subscribe((users) => {
            this.$userSuggestions.set(users);
        });
    }

    /**
     * TODO: wire to POST /v1/roles/{roleId}/users/{userId} once #36937 lands.
     * The dedicated grant endpoint does not exist yet; the popover UI is
     * shown so the flow is testable but the confirm button surfaces the gap.
     */
    protected onGrantUser(_user: UserFilterResult, panel: Popover): void {
        console.warn(
            '[dot-roles] grant user placeholder — waiting on #36937 (POST /v1/roles/{roleId}/users/{userId})'
        );
        panel.hide();
    }

    /**
     * TODO: wire to DELETE /v1/roles/{roleId}/users once #36938 lands.
     * Bulk remove is intentionally disabled here so QA cannot accidentally
     * exercise the not-yet-implemented flow against real data.
     */
    protected onRemoveSelected(): void {
        console.warn(
            '[dot-roles] bulk remove placeholder — waiting on #36938 (DELETE /v1/roles/{roleId}/users)'
        );
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
