import { Subject, of } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    computed,
    effect,
    inject,
    signal,
    untracked
} from '@angular/core';
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

import { catchError, debounceTime, switchMap, tap } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotMessageService,
    DotRoleUserResult
} from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotRoleMember } from '../../../models/dot-roles.models';
import { DotRolesPortletService } from '../../../services/dot-roles-portlet.service';
import { DotRolesStore } from '../../store/dot-roles.store';

/** How long the "just granted" row highlight stays before fading out. */
const GRANT_HIGHLIGHT_DURATION_MS = 3000;

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
    host: { class: 'block h-full' },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotRoleUsersTabComponent {
    protected readonly store = inject(DotRolesStore);
    readonly #service = inject(DotRolesPortletService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #messageService = inject(DotMessageService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #destroyRef = inject(DestroyRef);

    protected readonly rowsPerPageOptions = MEMBERS_ROWS_PER_PAGE_OPTIONS;
    protected readonly defaultRowsPerPage = MEMBERS_DEFAULT_ROWS_PER_PAGE;

    protected readonly $userSuggestions = signal<DotRoleUserResult[]>([]);
    protected readonly $suggestionsLoading = signal(false);

    // Hide users already granted to this role (direct or inherited) — the BE
    // grant call is idempotent so re-adding would be a silent no-op.
    protected readonly $filteredSuggestions = computed<DotRoleUserResult[]>(() => {
        const alreadyGranted = new Set(this.store.members().map((m) => m.userId));

        return this.$userSuggestions().filter((u) => !alreadyGranted.has(u.userId));
    });

    protected readonly $highlightUserId = signal<string | null>(null);
    #highlightTimeout: ReturnType<typeof setTimeout> | null = null;
    /** Flipped by `#destroyRef.onDestroy` to gate writes from stale `.then`s. */
    #destroyed = false;

    // Server-side search: client-side `p-listbox [filter]` only narrows the
    // page the server returned, so we refetch on every keystroke. `switchMap`
    // cancels in-flight requests as new keys land.
    readonly #userSearchInput$ = new Subject<string>();

    constructor() {
        effect(() => {
            const selectedRole = this.store.selectedRole();
            if (!selectedRole) {
                return;
            }
            untracked(() => this.store.loadMembers({ id: selectedRole.id }));
        });

        // `catchError → of([])` keeps the outer subscription alive after a
        // failed request so the next keystroke still triggers a search.
        // No `distinctUntilChanged` — reopening the popover pushes `''`
        // again on purpose so the seed fetch runs every time (otherwise the
        // pipeline stalls at the second open and the skeleton is permanent).
        // `debounceTime` alone still coalesces keystrokes.
        this.#userSearchInput$
            .pipe(
                debounceTime(300),
                tap(() => this.$suggestionsLoading.set(true)),
                switchMap((query) =>
                    this.#service.searchUsers(query).pipe(
                        catchError((error) => {
                            // Route through the shared manager so a 500 (or any
                            // failure past the debounce) surfaces as a toast
                            // instead of masquerading as "No users found".
                            this.#httpErrorManager.handle(error);

                            return of<DotRoleUserResult[]>([]);
                        }),
                        tap(() => this.$suggestionsLoading.set(false))
                    )
                ),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((users) => this.$userSuggestions.set(users));

        this.#destroyRef.onDestroy(() => {
            this.#destroyed = true;
            if (this.#highlightTimeout !== null) {
                clearTimeout(this.#highlightTimeout);
            }
        });
    }

    // Seeds the popover with the first page of users before the admin types.
    // `$suggestionsLoading` is flipped ON synchronously so the skeleton
    // shows immediately — the pipeline `tap` that sets it only fires after
    // the 300ms debounce, which would otherwise leave the empty-state
    // flashing before the initial fetch resolves. `$userSuggestions` is
    // reset so any leftover list from a previous open doesn't render
    // stale rows underneath the skeleton.
    protected onGrantPanelShow(): void {
        this.$suggestionsLoading.set(true);
        this.$userSuggestions.set([]);
        this.#userSearchInput$.next('');
    }

    protected onGrantSearch(event: ListboxFilterEvent): void {
        this.#userSearchInput$.next(event.filter ?? '');
    }

    // Restart (don't stack) the highlight timer on back-to-back grants —
    // last grant wins the fade window.
    protected onGrantUser(user: DotRoleUserResult, panel: Popover): void {
        panel.hide();
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

    /** Direct grants only — inherited rows must be revoked at the ancestor. */
    protected isDirectGrant(member: DotRoleMember): boolean {
        return member.grantedFromRoleId === this.store.selectedRoleId();
    }

    // Avatar initials with fallbacks to the email initial, then `?`, so we
    // never render an empty avatar for users missing first/last name.
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
}
