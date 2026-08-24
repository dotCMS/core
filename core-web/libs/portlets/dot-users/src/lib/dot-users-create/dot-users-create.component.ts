import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    signal,
    untracked
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { TabsModule } from 'primeng/tabs';
import { TagModule } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotUsersFormGroup, passwordsMatchValidator } from './dot-users-form.model';
import { DotUsersCreateStore } from './store/dot-users-create.store';
import { DotUsersProfileTabComponent } from './tabs/dot-users-profile-tab/dot-users-profile-tab.component';
import { DotUsersRolesTabComponent } from './tabs/dot-users-roles-tab/dot-users-roles-tab.component';

import { DotUsersReplacementPickerComponent } from '../components/dot-users-replacement-picker/dot-users-replacement-picker.component';
import { DotUserFormPayload, DotUserListItem } from '../services/dot-users.service';

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
 * Scope for issue #36718 — Profile + Roles tabs are real; Permissions
 * and API Tokens render "Coming soon" placeholders and are delivered
 * by #36719 and #36720.
 */
@Component({
    selector: 'dot-users-create',
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        AvatarModule,
        ButtonModule,
        DialogModule,
        InputTextModule,
        SkeletonModule,
        TabsModule,
        TagModule,
        DotMessagePipe,
        DotUsersReplacementPickerComponent,
        DotUsersProfileTabComponent,
        DotUsersRolesTabComponent
    ],
    templateUrl: './dot-users-create.component.html',
    styleUrl: './dot-users-create.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex h-full min-h-0 flex-col' },
    // Dialog-scoped store: each dialog instance gets its own hydration
    // pipeline, keeping HTTP + status out of the component body.
    providers: [DotUsersCreateStore]
})
export class DotUsersCreateComponent {
    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #config = inject<DynamicDialogConfig<DialogData>>(DynamicDialogConfig);
    readonly #fb = inject(FormBuilder);
    readonly #messageService = inject(DotMessageService);
    readonly #store = inject(DotUsersCreateStore);

    readonly user = this.#config.data?.user ?? null;
    readonly isEdit = !!this.user;

    readonly form: DotUsersFormGroup = this.#fb.nonNullable.group({
        account: this.#fb.nonNullable.group(
            {
                firstName: ['', [Validators.required]],
                lastName: ['', [Validators.required]],
                email: ['', [Validators.required, Validators.email]],
                // minLength runs in both modes — the passwordsMatchValidator on
                // the group treats an empty password as "keep current" for edit,
                // so a 1-char value can't slip through on either flow.
                password: ['', [Validators.minLength(6)]],
                confirmPassword: [''],
                active: [true]
            },
            { validators: [passwordsMatchValidator] }
        ),
        additionalInfo: this.#fb.nonNullable.group({
            prefix: [''],
            suffix: [''],
            title: [''],
            company: [''],
            website: ['']
        }),
        access: this.#fb.nonNullable.group({
            cmsAdmin: [false],
            backend: [true],
            frontend: [false],
            showGettingStarted: [true]
        })
    });

    /**
     * Signal mirror of `form.controls.access.valueChanges`. Drives the
     * header `Can login to Admin UI` chip reactively — the value is
     * derived from CMS Admin || Back-end User, so the chip appears
     * and disappears as those toggles change without any manual
     * change detection.
     */
    readonly #$accessValue = toSignal(this.form.controls.access.valueChanges, {
        initialValue: this.form.controls.access.getRawValue()
    });

    /**
     * Whether the user has console access. Derived on the backend as
     * `admin OR backendUser`, so we mirror the same rule locally —
     * lets the header chip react instantly to Access toggle changes
     * without waiting for a save round-trip.
     */
    readonly $canLoginToAdmin = computed(() => {
        const access = this.#$accessValue();

        return !!access.cmsAdmin || !!access.backend;
    });

    /**
     * Signal mirror of `form.controls.account.valueChanges`. Used to
     * derive the header (name + initials) reactively without wiring up
     * change detection manually. `toSignal` starts from the current
     * form value so the very first render already reflects the reset
     * we ran below.
     */
    readonly #$accountValue = toSignal(this.form.controls.account.valueChanges, {
        initialValue: this.form.controls.account.getRawValue()
    });

    readonly $displayName = computed(() => {
        const account = this.#$accountValue();
        const first = (account.firstName ?? '').trim();
        const last = (account.lastName ?? '').trim();
        const combined = `${first} ${last}`.trim();

        if (combined) {
            return combined;
        }

        return this.isEdit
            ? this.#messageService.get('users.dialog.untitled-user')
            : this.#messageService.get('users.dialog.new-user');
    });

    readonly $initials = computed(() => {
        const account = this.#$accountValue();
        const first = (account.firstName ?? '').charAt(0);
        const last = (account.lastName ?? '').charAt(0);
        const value = `${first}${last}`.toUpperCase();

        return value || (this.isEdit ? '?' : 'NU');
    });

    readonly $isActive = computed(() => Boolean(this.#$accountValue().active));

    protected readonly $activeTab = signal(0);
    protected readonly $deleteConfirmVisible = signal(false);
    protected readonly $deleteConfirmationInput = signal('');
    protected readonly $replacementUser = signal<DotUserListItem | null>(null);
    /**
     * Flips on when the user clicks Delete without a valid form.
     * Used to reveal a footer validation message; the button itself
     * stays enabled per design convention (no disabled buttons in UI).
     * Reset every time the picker or email input changes so the user
     * gets a fresh state after they course-correct.
     */
    protected readonly $deleteAttempted = signal(false);

    /**
     * Hydration signals surfaced from the dialog-scoped store — the
     * shell just reads them; the store owns the forkJoin + status.
     */
    protected readonly $isLoading = computed(() => this.#store.status() === 'loading');
    /**
     * Signals that the initial data is fully hydrated — profile fields,
     * assigned roles, and the getting-started state. In create mode
     * we short-circuit to `true` because there's nothing to load. Save
     * is disabled until this flips to `true`.
     */
    protected readonly $dataReady = computed(
        () => !this.isEdit || this.#store.status() === 'loaded'
    );

    protected readonly $isSaveDisabled = computed(() => !this.$dataReady());

    /**
     * Role KEYS that hydrate the Roles tab's Granted panel. Sourced
     * from `getUserRoles` on edit-mode open; stays empty in create
     * mode.
     */
    protected readonly initialGrantedRoleKeys = signal<string[]>([]);

    /**
     * Latest snapshot from the Roles tab. Populated on every
     * `grantedChange` emission and used to build the outbound
     * `roles` field on save.
     */
    private readonly currentRoleKeys = signal<string[] | null>(null);

    /**
     * ID list handed to the replacement picker so the user being
     * deleted is filtered out of the suggestion pool. Backed by a
     * getter so it stays trivially derivable from `this.user`.
     */
    protected readonly excludedReplacementIds = this.user?.userId ? [this.user.userId] : [];

    /**
     * Granular per-field validity for the delete confirm dialog. The
     * error strings are surfaced under each field only after the user
     * has actually tried to click Delete (`$deleteAttempted`) so an
     * untouched form stays clean-looking on open.
     */
    protected readonly $replacementError = computed(() => {
        if (!this.$deleteAttempted()) {
            return null;
        }

        const replacement = this.$replacementUser();
        if (!replacement) {
            return 'users.dialog.delete-confirm.replacement.required';
        }
        if (replacement.userId === this.user?.userId) {
            return 'users.dialog.delete-confirm.replacement.self';
        }

        return null;
    });

    protected readonly $emailConfirmError = computed(() => {
        if (!this.$deleteAttempted()) {
            return null;
        }

        const input = this.$deleteConfirmationInput().trim().toLowerCase();
        if (!input) {
            return 'users.dialog.delete-confirm.confirm.required';
        }

        const target = (this.user?.emailAddress ?? '').trim().toLowerCase();
        if (target && input !== target) {
            return 'users.dialog.delete-confirm.confirm.mismatch';
        }

        return null;
    });

    protected readonly $canConfirmDelete = computed(() => {
        const target = (this.user?.emailAddress ?? '').trim().toLowerCase();
        if (!target) {
            return false;
        }

        const emailMatches = this.$deleteConfirmationInput().trim().toLowerCase() === target;
        const replacement = this.$replacementUser();
        const replacementValid = !!replacement && replacement.userId !== this.user?.userId;

        return emailMatches && replacementValid;
    });

    constructor() {
        if (this.user) {
            this.hydrateFromListItem(this.user);
            this.#store.loadUserDetail(this.user.userId);
        } else {
            this.enableCreatePasswordValidators();
        }

        // Watch the store for status transitions the shell has to
        // react to: `loaded` triggers form patching from the fetched
        // detail; `error` closes the dialog after the shared HTTP
        // error manager surfaces its toast (the store handles the
        // toast, we handle the dialog lifecycle).
        effect(() => {
            const status = this.#store.status();
            if (status === 'loaded') {
                untracked(() => this.applyLoadedDetail());
            } else if (status === 'error') {
                untracked(() => this.#dialogRef.close());
            }
        });
    }

    protected close(): void {
        this.#dialogRef.close();
    }

    protected save(): void {
        this.form.markAllAsTouched();
        if (this.form.invalid || this.$isSaveDisabled()) {
            return;
        }

        const { payload, gettingStartedChange } = this.buildSavePayload();
        const result: DotUsersDialogResult = {
            action: 'save',
            mode: this.isEdit ? 'update' : 'create',
            payload,
            gettingStartedChange
        };

        this.#dialogRef.close(result);
    }

    protected openDeleteConfirm(): void {
        this.$deleteConfirmationInput.set('');
        this.$replacementUser.set(null);
        this.$deleteAttempted.set(false);
        this.$deleteConfirmVisible.set(true);
    }

    protected closeDeleteConfirm(): void {
        this.$deleteConfirmVisible.set(false);
    }

    protected confirmDelete(): void {
        const replacement = this.$replacementUser();
        if (!this.$canConfirmDelete() || !this.user?.userId || !replacement) {
            // Surface the footer hint instead of silently swallowing the
            // click — matches the design convention of always-enabled
            // buttons + contextual validation.
            this.$deleteAttempted.set(true);

            return;
        }

        const result: DotUsersDialogResult = {
            action: 'delete',
            userId: this.user.userId,
            replacementUserId: replacement.userId
        };

        this.#dialogRef.close(result);
    }

    protected onDeleteInputChange(value: string): void {
        this.$deleteConfirmationInput.set(value);
        this.$deleteAttempted.set(false);
    }

    protected onReplacementSelect(user: DotUserListItem | null): void {
        this.$replacementUser.set(user);
        this.$deleteAttempted.set(false);
    }

    /**
     * Fills the form with the fields the list row already knows about so
     * the dialog opens with the correct name / email / status even
     * before {@link loadUserDetail} finishes the round-trip.
     */
    private hydrateFromListItem(item: DotUserListItem): void {
        this.form.patchValue({
            account: {
                firstName: item.firstName ?? '',
                lastName: item.lastName ?? '',
                email: item.emailAddress ?? '',
                active: item.active ?? true
            },
            access: {
                cmsAdmin: item.admin ?? false,
                backend: item.backendUser ?? false,
                frontend: item.frontendUser ?? false,
                showGettingStarted: false
            }
        });
    }

    /**
     * Patches the form with the fully-hydrated detail from the store.
     * Called once when status transitions to `loaded`.
     */
    private applyLoadedDetail(): void {
        const detail = this.#store.detail();
        if (!detail) {
            return;
        }
        const roleKeys = this.#store.roleKeys();
        const roleKeySet = new Set(roleKeys);
        const additionalInfo = this.#store.additionalInfo();

        this.form.patchValue({
            account: {
                firstName: detail.firstName ?? '',
                lastName: detail.lastName ?? '',
                email: detail.emailAddress ?? '',
                active: detail.active ?? true
            },
            additionalInfo: {
                prefix: (additionalInfo['prefix'] as string) ?? '',
                suffix: (additionalInfo['suffix'] as string) ?? '',
                title: (additionalInfo['title'] as string) ?? '',
                company: (additionalInfo['company'] as string) ?? '',
                website: (additionalInfo['website'] as string) ?? ''
            },
            access: {
                cmsAdmin: roleKeySet.has(ACCESS_ROLE_KEYS.cmsAdmin),
                backend: roleKeySet.has(ACCESS_ROLE_KEYS.backend),
                frontend: roleKeySet.has(ACCESS_ROLE_KEYS.frontend),
                showGettingStarted: this.#store.gettingStarted()
            }
        });
        this.form.markAsPristine();

        // Seed the Roles-tab integration signals so the Granted panel
        // opens with the user's current membership, and save picks up
        // the same list if the user never touches the tab.
        this.initialGrantedRoleKeys.set(roleKeys);
        this.currentRoleKeys.set(roleKeys);
    }

    protected onGrantedRolesChange(keys: string[]): void {
        this.currentRoleKeys.set(keys);
    }

    private enableCreatePasswordValidators(): void {
        const account = this.form.controls.account;
        // Layer `required` on top of the always-on `minLength(6)` so
        // create mode rejects empty strings.
        account.controls.password.setValidators([Validators.required, Validators.minLength(6)]);
        account.controls.confirmPassword.setValidators([Validators.required]);
        account.controls.password.updateValueAndValidity({ emitEvent: false });
        account.controls.confirmPassword.updateValueAndValidity({ emitEvent: false });
    }

    /**
     * Builds the backend {@link DotUserFormPayload} plus a
     * `gettingStartedChange` instruction for the store to chain.
     *
     * When the Roles tab has taken ownership of role membership (via
     * `grantedChange`, tracked in `currentRoleKeys`), its snapshot is
     * the source of truth for `payload.roles`. Otherwise we fall back
     * to `mergeRoleKeysForSave`, which composes the outbound list
     * from cached role keys + access-toggle deltas — safe when the
     * user never opened the Roles tab.
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
        const access = raw.access;

        // Backend replaces `additionalInfo` wholesale (see
        // UserResource#save), so we spread the loaded map first to keep
        // any keys the profile tab does not surface, then overlay the
        // five managed fields. We always write those five — an empty
        // string wins over the stale value that would otherwise stick.
        const additionalInfo: Record<string, unknown> = { ...this.#store.additionalInfo() };
        for (const key of ADDITIONAL_INFO_KEYS) {
            additionalInfo[key] = (additionalInfoValue[key] ?? '').trim();
        }

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

        payload.additionalInfo = additionalInfo;

        // Compose the outbound role list from the "base" role keys
        // (Roles tab's Granted snapshot when the user touched it,
        // otherwise the store's fetched roleKeys) with the current
        // access-toggle deltas applied on top. `mergeRoleKeysForSave`
        // strips the three access-role keys from the base and re-adds
        // whichever toggles are ON, so an Access flip is always
        // reflected in the payload regardless of Roles-tab state.
        payload.roles = this.mergeRoleKeysForSave(access);

        let gettingStartedChange: 'add' | 'remove' | undefined;
        if (access.showGettingStarted !== this.#store.gettingStarted()) {
            gettingStartedChange = access.showGettingStarted ? 'add' : 'remove';
        }

        return { payload, gettingStartedChange };
    }

    /**
     * Strips the user's implicit personal role (roleKey === userId)
     * from the outbound list. `UserResource#processRoles` first calls
     * `removeAllRolesFromUser` (no `editUsers` guard) and then tries
     * to re-add every key in the payload. Re-adding the personal role
     * fails at `RoleAPIImpl.addRoleToUser` because it has
     * `editUsers=false`, and the exception rolls the whole save back
     * with `"Cannot alter users on this role"`. Leaving that key out
     * of the payload keeps the save from tripping the guard.
     */
    private filterOutgoingRoleKeys(keys: readonly string[]): string[] {
        const personalRoleKey = this.user?.userId ?? '';
        if (!personalRoleKey) {
            return [...keys];
        }

        return keys.filter((key) => key !== personalRoleKey);
    }

    /**
     * Merges the "base" role KEYS with the current Access toggles.
     * The base is the Roles tab's Granted snapshot (`currentRoleKeys`)
     * when the user touched it — otherwise the store's fetched keys.
     * We strip the three access-role slots from the base list and
     * add back whichever toggles are ON, so an Access flip is always
     * captured on save even when the user visited the Roles tab.
     */
    private mergeRoleKeysForSave(access: {
        cmsAdmin: boolean;
        backend: boolean;
        frontend: boolean;
    }): string[] {
        const base = this.currentRoleKeys() ?? this.#store.roleKeys();
        const accessKeys = new Set<string>(Object.values(ACCESS_ROLE_KEYS));
        const nonAccess = base.filter((key) => !accessKeys.has(key));
        const merged = new Set(nonAccess);

        if (access.cmsAdmin) merged.add(ACCESS_ROLE_KEYS.cmsAdmin);
        if (access.backend) merged.add(ACCESS_ROLE_KEYS.backend);
        if (access.frontend) merged.add(ACCESS_ROLE_KEYS.frontend);

        return this.filterOutgoingRoleKeys(Array.from(merged));
    }
}
