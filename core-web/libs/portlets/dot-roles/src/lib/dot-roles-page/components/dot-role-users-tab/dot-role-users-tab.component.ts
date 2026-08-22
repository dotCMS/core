import { Observable, Subject, of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ListboxFilterEvent, ListboxModule } from 'primeng/listbox';
import { Popover, PopoverModule } from 'primeng/popover';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import {
    catchError,
    debounceTime,
    distinctUntilChanged,
    map,
    switchMap,
    tap
} from 'rxjs/operators';

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

/**
 * How long the "just granted" row highlight stays before it fades back
 * to the default row background. Long enough to catch the admin's eye
 * in a scrolled list, short enough not to linger through the next
 * interaction.
 */
const GRANT_HIGHLIGHT_DURATION_MS = 3000;

/**
 * Page-size dropdown for the members table. The current members endpoints
 * (`/api/v1/users/filter?roleKey=…` and the legacy
 * `/v1/roles/{id}/rolehierarchyanduserroles` fallback) return the entire
 * member list in a single response, so this pagination is client-side —
 * p-table slices `store.members()` itself. When #37070 ships a server-paged
 * `GET /v1/roles/{roleId}/users` we can switch to `[lazy]="true"` and drive
 * it from the store; the visual pattern (bottom paginator + rows-per-page)
 * matches publishing-queue-beta so users see the same control across
 * portlets.
 */
const MEMBERS_ROWS_PER_PAGE_OPTIONS = [20, 40, 60] as const;
const MEMBERS_DEFAULT_ROWS_PER_PAGE = 20;

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
        SkeletonModule,
        DotMessagePipe
    ],
    providers: [ConfirmationService],
    templateUrl: './dot-role-users-tab.component.html',
    host: { class: 'block h-full' }
})
export class DotRoleUsersTabComponent {
    protected readonly store = inject(DotRolesStore);
    readonly #http = inject(HttpClient);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #messageService = inject(DotMessageService);
    readonly #destroyRef = inject(DestroyRef);

    /** Static config exposed to the template — see `MEMBERS_ROWS_PER_PAGE_OPTIONS`. */
    protected readonly rowsPerPageOptions = MEMBERS_ROWS_PER_PAGE_OPTIONS;
    protected readonly defaultRowsPerPage = MEMBERS_DEFAULT_ROWS_PER_PAGE;

    protected readonly $userSuggestions = signal<UserFilterResult[]>([]);
    /** True while `/api/v1/users/filter` is in flight — drives the popover's skeleton state. */
    protected readonly $suggestionsLoading = signal(false);

    /**
     * Suggestions shown in the Grant popover, minus users who already
     * belong to the current role — whether directly granted or inherited
     * from an ancestor. Direct grants would be a duplicate; inherited
     * grants would be a silent no-op on the BE (see
     * `POST /v1/roles/{roleId}/users/{userId}` behavior notes), so
     * hiding both keeps the picker honest.
     */
    protected readonly $filteredSuggestions = computed<UserFilterResult[]>(() => {
        const alreadyGranted = new Set(this.store.members().map((m) => m.userId));

        return this.$userSuggestions().filter((u) => !alreadyGranted.has(u.userId));
    });

    /**
     * User id whose row should render with the "just granted" highlight
     * (a fading background so the admin can spot the row they just added
     * even if the list is long). Set by `onGrantUser` when the store
     * promise resolves, cleared by a 4s timer.
     */
    protected readonly $highlightUserId = signal<string | null>(null);
    #highlightTimeout: ReturnType<typeof setTimeout> | null = null;

    /**
     * Debounced pipe that hits `/api/v1/users/filter?query=X` as the admin
     * types in the Grant popover's search input. Client-side filtering
     * (`p-listbox [filter]`) only narrows the FIRST page the server sent
     * — with hundreds of users the target may never land in that page —
     * so we refetch on every keystroke after a 300ms debounce and
     * `switchMap` cancels in-flight requests as new keys land.
     */
    readonly #userSearchInput$ = new Subject<string>();

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

        // Wire the debounced user search — see `#userSearchInput$` doc.
        // `tap` before `switchMap` flips loading ON so the popover shows
        // its skeleton state; the inner `tap` flips it OFF once the
        // response (or error fallback) resolves. `catchError → of([])`
        // keeps the outer subscription alive after a failed request.
        this.#userSearchInput$
            .pipe(
                debounceTime(300),
                distinctUntilChanged(),
                tap(() => this.$suggestionsLoading.set(true)),
                switchMap((query) =>
                    this.#getUserSuggestions(query).pipe(
                        catchError(() => of<UserFilterResult[]>([])),
                        tap(() => this.$suggestionsLoading.set(false))
                    )
                ),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((users) => this.$userSuggestions.set(users));

        // Clear the highlight timer if the component is torn down before
        // the 4s window elapses (route away, role switch, etc.).
        this.#destroyRef.onDestroy(() => {
            if (this.#highlightTimeout !== null) {
                clearTimeout(this.#highlightTimeout);
            }
        });
    }

    /**
     * Popover open hook — seeds the listbox with the first page of users
     * (no query) so the admin sees a browsable list immediately, before
     * typing. Subsequent keystrokes flow through `onGrantSearch` which
     * refetches server-side.
     */
    protected onGrantPanelShow(): void {
        this.#userSearchInput$.next('');
    }

    /**
     * Bound to `p-listbox`'s `(onFilter)` — fires as the admin types in
     * the popover's search input. Forwards the query to the debounced
     * server-search pipe. The listbox's internal client-side filter
     * still runs on top of what the server returns, which is harmless
     * (server results already match the query).
     */
    protected onGrantSearch(event: ListboxFilterEvent): void {
        this.#userSearchInput$.next(event.filter ?? '');
    }

    /**
     * Grant the picked user to the currently-selected role via
     * `POST /v1/roles/{roleId}/users/{userId}` (issue #36937). The BE is
     * idempotent — re-granting a role the user already holds is a no-op — so
     * we don't have to gate on client-side dedup. On success the store
     * refreshes the members list so the new row lands in the table with the
     * correct `grantedFromRoleId` labelling.
     *
     * The new row is highlighted for 4 seconds so the admin can spot it in
     * a long list without hunting. `#highlightTimeout` is tracked so back-
     * to-back grants restart the timer instead of stacking (last grant wins).
     */
    protected onGrantUser(user: UserFilterResult, panel: Popover): void {
        panel.hide();
        this.store.grantUserToRole(user.userId).then((result) => {
            if (!result?.granted) {
                return;
            }
            this.$highlightUserId.set(user.userId);
            if (this.#highlightTimeout !== null) {
                clearTimeout(this.#highlightTimeout);
            }
            this.#highlightTimeout = setTimeout(() => {
                this.$highlightUserId.set(null);
                this.#highlightTimeout = null;
            }, GRANT_HIGHLIGHT_DURATION_MS);
        });
    }

    /**
     * Row-level remove: revoke a single member's direct grant from the
     * current role (DELETE /v1/roles/{roleId}/users — issue #36938,
     * called with a one-item `userIds` array). Only wired for direct
     * grants; inherited rows never render this button. Confirms first
     * so the destructive action isn't a hover-hit-away from firing.
     */
    protected onRemoveMember(member: DotRoleMember): void {
        this.#confirmationService.confirm({
            message: this.#messageService.get(
                'roles.users.confirm.remove.message',
                `${member.firstName} ${member.lastName}`.trim() || member.emailAddress
            ),
            header: this.#messageService.get('roles.users.confirm.remove.header'),
            acceptLabel: this.#messageService.get('roles.users.remove'),
            rejectLabel: this.#messageService.get('roles.action.cancel'),
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            position: 'center',
            accept: () => {
                this.store.removeUsersFromRole([member.userId]);
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

    #getUserSuggestions(query: string): Observable<UserFilterResult[]> {
        const q = query ? `?query=${encodeURIComponent(query)}` : '';
        return this.#http
            .get<DotCMSResponse<UserFilterResult[]>>(`/api/v1/users/filter${q}`)
            .pipe(map((response) => response.entity ?? []));
    }
}
