import { Subject } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';

import { debounceTime } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotMessageService,
    DotUserSearchRow
} from '@dotcms/data-access';
import {
    DotEmptyContainerComponent,
    DotMessagePipe,
    DotUserPickerComponent,
    PrincipalConfiguration
} from '@dotcms/ui';

import { DotRoleMember } from '../../../models/dot-roles.models';
import { DotRolesStore } from '../../store/dot-roles.store';

/** How long the "just granted" row highlight stays before fading out. */
const GRANT_HIGHLIGHT_DURATION_MS = 3000;

/** Idle time before a typed member search is sent. Matches the other listing search boxes. */
const MEMBER_SEARCH_DEBOUNCE_MS = 300;

// Members are paginated client-side. `GET /v1/roles/{roleId}/users` (#37070)
// is server-paged, but the rows shown here are the *union* of the selected
// role's direct grants and everything inherited from its ancestors, merged
// and de-duplicated in the store. A server page of one ancestor is not a page
// of that union, so each ancestor is pulled whole (`ROLE_MEMBERS_PAGE_SIZE`)
// and `p-table` pages the merged array. See the note on that constant.
const MEMBERS_ROWS_PER_PAGE_OPTIONS = [20, 40, 60] as const;
const MEMBERS_DEFAULT_ROWS_PER_PAGE = 20;

@Component({
    selector: 'dot-role-users-tab',
    imports: [
        FormsModule,
        ButtonModule,
        TableModule,
        TagModule,
        ToolbarModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        TooltipModule,
        ConfirmDialogModule,
        SkeletonModule,
        DotMessagePipe,
        DotEmptyContainerComponent,
        DotUserPickerComponent
    ],
    providers: [ConfirmationService],
    templateUrl: './dot-role-users-tab.component.html',
    host: { class: 'block h-full' }
})
export class DotRoleUsersTabComponent {
    protected readonly store = inject(DotRolesStore);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #messageService = inject(DotMessageService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #destroyRef = inject(DestroyRef);

    // `hideContactUsLink` is set at every call site: this is an internal admin
    // screen, not a licensing dead-end, so the contact link would be noise.
    protected readonly $emptyMembersConfig = computed<PrincipalConfiguration>(() => ({
        title: this.#messageService.get('roles.users.empty.title'),
        subtitle: this.#messageService.get('roles.users.empty.copy'),
        icon: 'group',
        iconStyle: 'material-symbols-rounded'
    }));

    protected readonly $noSearchResultsConfig = computed<PrincipalConfiguration>(() => ({
        title: this.#messageService.get('roles.users.filter.empty.title'),
        icon: 'search_off',
        iconStyle: 'material-symbols-rounded'
    }));

    /**
     * What is typed in the toolbar's search box. Kept here rather than bound to
     * the store's `membersFilter`, which is trimmed and debounced: writing that
     * back into the input would eat the space the admin just typed.
     */
    protected readonly $memberSearch = signal('');
    readonly #memberSearchInput$ = new Subject<string>();

    protected readonly rowsPerPageOptions = MEMBERS_ROWS_PER_PAGE_OPTIONS;
    protected readonly defaultRowsPerPage = MEMBERS_DEFAULT_ROWS_PER_PAGE;

    /**
     * Users the picker leaves out because they already hold the role, directly or inherited —
     * the grant is idempotent, so offering them would be a silent no-op.
     */
    protected readonly $memberIds = computed(() => this.store.members().map((m) => m.userId));

    protected readonly $highlightUserId = signal<string | null>(null);
    #highlightTimeout: ReturnType<typeof setTimeout> | null = null;
    /** Flipped by `#destroyRef.onDestroy` to gate writes from stale `.then`s. */
    #destroyed = false;

    constructor() {
        effect(() => {
            const selectedRole = this.store.selectedRole();
            if (!selectedRole) {
                return;
            }
            untracked(() => this.store.loadMembers({ id: selectedRole.id }));
        });

        // The store drops the search when another role is selected; clear the
        // box to match. Keyed on the id so saving an edit to the same role —
        // which replaces `selectedRole` — leaves the search alone.
        effect(() => {
            this.store.selectedRoleId();
            untracked(() => this.$memberSearch.set(''));
        });

        // No `distinctUntilChanged`: after a role switch the store has already
        // dropped the term, so retyping it must go through. The store skips a
        // term equal to the one it holds.
        this.#memberSearchInput$
            .pipe(debounceTime(MEMBER_SEARCH_DEBOUNCE_MS), takeUntilDestroyed(this.#destroyRef))
            .subscribe((term) => this.store.setMembersFilter(term));

        this.#destroyRef.onDestroy(() => {
            this.#destroyed = true;
            if (this.#highlightTimeout !== null) {
                clearTimeout(this.#highlightTimeout);
            }
        });
    }

    protected onMemberSearch(term: string): void {
        this.$memberSearch.set(term);
        this.#memberSearchInput$.next(term);
    }

    /**
     * Grants the role to whoever was picked. Restarts (doesn't stack) the highlight timer on
     * back-to-back grants — the last grant wins the fade window.
     */
    protected onGrantUsers(users: DotUserSearchRow[]): void {
        for (const user of users) {
            this.store.grantUserToRole(user.userId).then((result) => {
                // If the user navigated away between the click and the response,
                // don't touch signals on a torn-down component — the effect that
                // consumes them is gone.
                if (this.#destroyed || !result?.granted) {
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
    }

    /** The picker cannot report errors itself (it lives in `@dotcms/ui`); route them here. */
    protected onDirectoryError(error: HttpErrorResponse): void {
        this.#httpErrorManager.handle(error);
    }

    // Confirms before firing so the destructive row action isn't a
    // hover-hit-away. Only rendered for direct grants (inherited rows
    // must be revoked from the ancestor role).
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

    // See the note on `trackByGroupId` in the Tools tab: PrimeNG tracks rows
    // by object identity, so re-fetching members after a grant/remove would
    // otherwise tear down and rebuild every row — dropping the `just-granted`
    // highlight's transition and re-triggering row animations.
    protected readonly trackByUserId = (_index: number, member: DotRoleMember): string =>
        member.userId;

    /** Direct grants only — inherited rows must be revoked at the ancestor. */
    protected isDirectGrant(member: DotRoleMember): boolean {
        return member.grantedFromRoleId === this.store.selectedRoleId();
    }
}
