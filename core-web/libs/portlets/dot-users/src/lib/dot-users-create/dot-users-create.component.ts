import { forkJoin, of } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    inject,
    signal
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { TabsModule } from 'primeng/tabs';
import { TagModule } from 'primeng/tag';

import { catchError, finalize, take } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { passwordsMatchValidator } from './dot-users-form.model';
import { DotUsersProfileTabComponent } from './tabs/dot-users-profile-tab/dot-users-profile-tab.component';

import { DotUsersReplacementPickerComponent } from '../components/dot-users-replacement-picker/dot-users-replacement-picker.component';
import {
    DotUserFormPayload,
    DotUserListItem,
    DotUsersService
} from '../services/dot-users.service';

interface DialogData {
    user?: DotUserListItem;
}

/**
 * Payload the list component consumes on `DynamicDialogRef.onClose`.
 * The `action` discriminant tells the list which store method to
 * dispatch — the shape stays flat so the list can add cases (e.g.
 * bulk actions) without breaking the union.
 *
 * `gettingStartedChange` is set only when the Show Getting Started
 * toggle diverges from the initial state; the store chains a
 * separate toolgroup PUT after the primary save.
 */
export type DotUsersDialogResult =
    | {
          action: 'save';
          mode: 'create' | 'update';
          payload: DotUserFormPayload;
          gettingStartedChange?: 'add' | 'remove';
      }
    | { action: 'delete'; userId: string; replacementUserId?: string };

/**
 * Additional-info keys the Profile tab surfaces as first-class fields.
 * These are conventional dotCMS keys — the backend stores them inside
 * `User.additionalInfo` (a free-form map) and returns them unchanged.
 */
const ADDITIONAL_INFO_KEYS = ['prefix', 'suffix', 'title', 'company', 'website'] as const;

/**
 * Well-known role keys that back the three editable Access toggles.
 * The `PUT /api/v1/users` endpoint identifies roles by `roleKey`, not
 * `id`, so we send these values directly in the outbound `roles` list.
 */
const ACCESS_ROLE_KEYS = {
    cmsAdmin: 'CMS Administrator',
    backend: 'DOTCMS_BACK_END_USER',
    frontend: 'DOTCMS_FRONT_END_USER'
} as const;

/**
 * Create / Edit User dialog. Hosts the four-tab experience (Profile,
 * Roles, Permissions, API Tokens) inside the shared PrimeNG dynamic
 * dialog frame. Owns the top-level reactive form, mode detection
 * (create vs edit), and orchestrates each tab as a standalone
 * presentational sub-component.
 *
 * Scope for issue #36717 — only the Profile tab is real. Roles,
 * Permissions, and API Tokens render "Coming soon" placeholders and
 * are delivered by their sibling issues (#36718, #36719, #36720),
 * each of which swaps its placeholder for the real tab component.
 */
@Component({
    selector: 'dot-users-create',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        AvatarModule,
        ButtonModule,
        DialogModule,
        InputTextModule,
        TabsModule,
        TagModule,
        DotMessagePipe,
        DotUsersReplacementPickerComponent,
        DotUsersProfileTabComponent
    ],
    templateUrl: './dot-users-create.component.html',
    styleUrl: './dot-users-create.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex h-full min-h-0 flex-col block' }
})
export class DotUsersCreateComponent {
    private readonly dialogRef = inject(DynamicDialogRef);
    private readonly config = inject<DynamicDialogConfig<DialogData>>(DynamicDialogConfig);
    private readonly fb = inject(FormBuilder);
    private readonly messageService = inject(DotMessageService);
    private readonly usersService = inject(DotUsersService);
    private readonly httpErrorManager = inject(DotHttpErrorManagerService);
    private readonly destroyRef = inject(DestroyRef);

    readonly user = this.config.data?.user ?? null;
    readonly isEdit = !!this.user;

    readonly form = this.fb.nonNullable.group({
        account: this.fb.nonNullable.group(
            {
                firstName: ['', [Validators.required]],
                lastName: ['', [Validators.required]],
                email: ['', [Validators.required, Validators.email]],
                password: [''],
                confirmPassword: [''],
                active: [true]
            },
            { validators: [passwordsMatchValidator] }
        ),
        additionalInfo: this.fb.nonNullable.group({
            prefix: [''],
            suffix: [''],
            title: [''],
            company: [''],
            website: ['']
        }),
        access: this.fb.nonNullable.group({
            cmsAdmin: [false],
            backend: [true],
            frontend: [false],
            canLogin: [true],
            showGettingStarted: [true]
        })
    });

    /**
     * Signal mirror of `form.controls.account.valueChanges`. Used to
     * derive the header (name + initials) reactively without wiring up
     * change detection manually. `toSignal` starts from the current
     * form value so the very first render already reflects the reset
     * we ran below.
     */
    private readonly accountValue = toSignal(this.form.controls.account.valueChanges, {
        initialValue: this.form.controls.account.getRawValue()
    });

    readonly displayName = computed(() => {
        const account = this.accountValue();
        const first = (account.firstName ?? '').trim();
        const last = (account.lastName ?? '').trim();
        const combined = `${first} ${last}`.trim();

        if (combined) {
            return combined;
        }

        return this.isEdit
            ? this.messageService.get('users.dialog.untitled-user')
            : this.messageService.get('users.dialog.new-user');
    });

    readonly initials = computed(() => {
        const account = this.accountValue();
        const first = (account.firstName ?? '').charAt(0);
        const last = (account.lastName ?? '').charAt(0);
        const value = `${first}${last}`.toUpperCase();

        return value || (this.isEdit ? '?' : 'NU');
    });

    readonly isActive = computed(() => Boolean(this.accountValue().active));

    protected readonly activeTab = signal(0);
    protected readonly deleteConfirmVisible = signal(false);
    protected readonly deleteConfirmationInput = signal('');
    protected readonly replacementUser = signal<DotUserListItem | null>(null);
    protected readonly isLoading = signal(false);

    /**
     * Full list of role KEYS the user currently holds — populated by
     * `GET /api/v1/roles/users/{userId}` on load. On save we start from
     * this list, strip the three access-role keys, then add back the
     * ones whose toggles are ON. This preserves every other role
     * membership (personal role, project-specific roles, etc.) since
     * the backend `PUT /api/v1/users` replaces the full role list.
     */
    private readonly loadedUserRoleKeys = signal<string[]>([]);

    /**
     * Snapshot of the `Show Getting Started` toggle at load time so
     * `buildPayload` can detect a diff and emit the appropriate
     * `add` / `remove` instruction for the toolgroup PUT.
     */
    private readonly initialGettingStarted = signal(false);

    /**
     * Signals that the initial data is fully hydrated — profile fields,
     * assigned roles, and the getting-started state. In create mode
     * we consider ourselves ready immediately (nothing to load). Save
     * is disabled until this flips to `true` so we never send a `roles`
     * list built from a stale, empty snapshot.
     */
    protected readonly dataReady = signal(false);

    protected readonly isSaveDisabled = computed(() => !this.dataReady());

    /**
     * ID list handed to the replacement picker so the user being
     * deleted is filtered out of the suggestion pool. Backed by a
     * getter so it stays trivially derivable from `this.user`.
     */
    protected readonly excludedReplacementIds = this.user?.userId ? [this.user.userId] : [];

    protected readonly canConfirmDelete = computed(() => {
        const target = (this.user?.emailAddress ?? '').trim().toLowerCase();
        if (!target) {
            return false;
        }

        const emailMatches = this.deleteConfirmationInput().trim().toLowerCase() === target;
        const replacement = this.replacementUser();
        const replacementValid = !!replacement && replacement.userId !== this.user?.userId;

        return emailMatches && replacementValid;
    });

    constructor() {
        if (this.user) {
            this.hydrateFromListItem(this.user);
            this.loadUserDetail(this.user.userId);
        } else {
            this.enableCreatePasswordValidators();
            this.dataReady.set(true);
        }
        this.disableCanLoginToggle();
        this.wireCanLoginDerivation();
    }

    protected close(): void {
        this.dialogRef.close();
    }

    protected save(): void {
        this.form.markAllAsTouched();
        if (this.form.invalid || this.isSaveDisabled()) {
            return;
        }

        const { payload, gettingStartedChange } = this.buildSavePayload();
        const result: DotUsersDialogResult = {
            action: 'save',
            mode: this.isEdit ? 'update' : 'create',
            payload,
            gettingStartedChange
        };

        this.dialogRef.close(result);
    }

    protected openDeleteConfirm(): void {
        this.deleteConfirmationInput.set('');
        this.replacementUser.set(null);
        this.deleteConfirmVisible.set(true);
    }

    protected closeDeleteConfirm(): void {
        this.deleteConfirmVisible.set(false);
    }

    protected confirmDelete(): void {
        const replacement = this.replacementUser();
        if (!this.canConfirmDelete() || !this.user?.userId || !replacement) {
            return;
        }

        const result: DotUsersDialogResult = {
            action: 'delete',
            userId: this.user.userId,
            replacementUserId: replacement.userId
        };

        this.dialogRef.close(result);
    }

    protected onDeleteInputChange(value: string): void {
        this.deleteConfirmationInput.set(value);
    }

    protected onReplacementSelect(user: DotUserListItem | null): void {
        this.replacementUser.set(user);
    }

    /**
     * Fills the form with the fields the list row already knows about so
     * the dialog opens with the correct name / email / status even
     * before {@link loadUserDetail} finishes the round-trip.
     */
    private hydrateFromListItem(item: DotUserListItem): void {
        const cmsAdmin = item.admin ?? false;
        const backend = item.backendUser ?? false;
        this.form.patchValue({
            account: {
                firstName: item.firstName ?? '',
                lastName: item.lastName ?? '',
                email: item.emailAddress ?? '',
                active: item.active ?? true
            },
            access: {
                cmsAdmin,
                backend,
                frontend: item.frontendUser ?? false,
                canLogin: cmsAdmin || backend,
                showGettingStarted: false
            }
        });
    }

    /**
     * Fetches everything the dialog needs to reach a fully-editable
     * state in edit mode:
     *   - `getUser` — full profile fields incl. additionalInfo
     *   - `getUserRoles` — role KEYS used to hydrate the Access toggles
     *     and to seed the "preserve other memberships" payload on save
     *   - `getGettingStartedState` — Show Getting Started initial value
     *
     * The getting-started call is wrapped in `catchError` because it's
     * a non-critical side surface — if it fails we default to `false`
     * and let the user toggle it manually. All three run in parallel;
     * the Save button stays disabled until every response has landed
     * (see `dataReady`).
     */
    private loadUserDetail(userId: string): void {
        this.isLoading.set(true);
        forkJoin({
            user: this.usersService.getUser(userId),
            userRoles: this.usersService.getUserRoles(userId),
            gettingStarted: this.usersService
                .getGettingStartedState(userId)
                .pipe(catchError(() => of(false)))
        })
            .pipe(
                take(1),
                finalize(() => this.isLoading.set(false)),
                takeUntilDestroyed(this.destroyRef)
            )
            .subscribe({
                next: ({ user, userRoles, gettingStarted }) => {
                    const roleKeys = userRoles
                        .map((role) => role.roleKey)
                        .filter((key): key is string => !!key);

                    this.loadedUserRoleKeys.set(roleKeys);
                    this.initialGettingStarted.set(gettingStarted);

                    const roleKeySet = new Set(roleKeys);
                    const additionalInfo = user.additionalInfo ?? {};

                    const cmsAdmin = roleKeySet.has(ACCESS_ROLE_KEYS.cmsAdmin);
                    const backend = roleKeySet.has(ACCESS_ROLE_KEYS.backend);

                    this.form.patchValue({
                        account: {
                            firstName: user.firstName ?? '',
                            lastName: user.lastName ?? '',
                            email: user.emailAddress ?? '',
                            active: user.active ?? true
                        },
                        additionalInfo: {
                            prefix: (additionalInfo['prefix'] as string) ?? '',
                            suffix: (additionalInfo['suffix'] as string) ?? '',
                            title: (additionalInfo['title'] as string) ?? '',
                            company: (additionalInfo['company'] as string) ?? '',
                            website: (additionalInfo['website'] as string) ?? ''
                        },
                        access: {
                            cmsAdmin,
                            backend,
                            frontend: roleKeySet.has(ACCESS_ROLE_KEYS.frontend),
                            canLogin: cmsAdmin || backend,
                            showGettingStarted: gettingStarted
                        }
                    });
                    this.form.markAsPristine();
                    this.dataReady.set(true);
                },
                error: (error) => this.httpErrorManager.handle(error)
            });
    }

    private enableCreatePasswordValidators(): void {
        const account = this.form.controls.account;
        account.controls.password.setValidators([Validators.required, Validators.minLength(6)]);
        account.controls.confirmPassword.setValidators([Validators.required]);
        account.controls.password.updateValueAndValidity({ emitEvent: false });
        account.controls.confirmPassword.updateValueAndValidity({ emitEvent: false });
    }

    /**
     * `Can Login to Admin UI` mirrors the backend's `hasConsoleAccess`
     * getter (`admin OR backendUser`). The property is derived, not
     * persisted — nothing to write to — so the toggle stays disabled
     * and instead tracks the two Access toggles that DO drive it.
     */
    private disableCanLoginToggle(): void {
        this.form.controls.access.controls.canLogin.disable({ emitEvent: false });
    }

    /**
     * Keeps `canLogin` in sync with the visible toggles that grant
     * console access. Any time `cmsAdmin` or `backend` changes we
     * recompute the disabled control's value so the user can see the
     * consequence of their pick without waiting for a save round-trip.
     *
     * `emitEvent: false` prevents this patch from re-firing the outer
     * `valueChanges` and looping.
     */
    private wireCanLoginDerivation(): void {
        const access = this.form.controls.access.controls;
        const update = () => {
            access.canLogin.setValue(!!access.cmsAdmin.value || !!access.backend.value, {
                emitEvent: false
            });
        };

        access.cmsAdmin.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(update);
        access.backend.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(update);
    }

    /**
     * Builds the backend {@link DotUserFormPayload} plus a
     * `gettingStartedChange` instruction for the store to chain.
     *
     * Roles are computed as: cached role-key list → strip the three
     * access-role keys → add back whichever access toggles are ON.
     * The result replaces the user's full role membership on the
     * backend (`UserResource#processRoles`), so leaving out the
     * non-access role keys would silently wipe them.
     *
     * Password and additionalInfo are omitted when empty so the
     * backend keeps their existing values.
     */
    private buildSavePayload(): {
        payload: DotUserFormPayload;
        gettingStartedChange?: 'add' | 'remove';
    } {
        const raw = this.form.getRawValue();
        const account = raw.account;
        const additionalInfoValue = raw.additionalInfo;
        // `access.canLogin` is a disabled control so it's absent from
        // getRawValue. We read the whole group directly to avoid an
        // undefined dereference on the toggles that DO participate.
        const access = this.form.controls.access.getRawValue();

        const additionalInfo = ADDITIONAL_INFO_KEYS.reduce<Record<string, string>>((acc, key) => {
            const value = (additionalInfoValue[key] ?? '').trim();
            if (value) {
                acc[key] = value;
            }

            return acc;
        }, {});

        const payload: DotUserFormPayload = {
            firstName: account.firstName.trim(),
            lastName: account.lastName.trim(),
            email: account.email.trim(),
            active: account.active
        };

        if (this.user?.userId) {
            payload.userId = this.user.userId;
        }

        const password = (account.password ?? '').trim();
        if (password) {
            payload.password = password;
        }

        if (Object.keys(additionalInfo).length > 0) {
            payload.additionalInfo = additionalInfo;
        }

        payload.roles = this.mergeRoleKeysForSave(access);

        let gettingStartedChange: 'add' | 'remove' | undefined;
        if (access.showGettingStarted !== this.initialGettingStarted()) {
            gettingStartedChange = access.showGettingStarted ? 'add' : 'remove';
        }

        return { payload, gettingStartedChange };
    }

    /**
     * Merges the cached role KEYS with the current Access toggles into
     * the list the backend expects. In create mode the cache is empty,
     * so the outbound list is just whichever access toggles are ON —
     * safe because create semantics ADD roles instead of replacing.
     */
    private mergeRoleKeysForSave(access: {
        cmsAdmin: boolean;
        backend: boolean;
        frontend: boolean;
    }): string[] {
        const accessKeys = new Set<string>(Object.values(ACCESS_ROLE_KEYS));
        const nonAccess = this.loadedUserRoleKeys().filter((key) => !accessKeys.has(key));
        const merged = new Set(nonAccess);

        if (access.cmsAdmin) merged.add(ACCESS_ROLE_KEYS.cmsAdmin);
        if (access.backend) merged.add(ACCESS_ROLE_KEYS.backend);
        if (access.frontend) merged.add(ACCESS_ROLE_KEYS.frontend);

        return Array.from(merged);
    }
}
