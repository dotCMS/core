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

import { finalize, take } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { passwordsMatchValidator } from './dot-users-form.model';
import { DotUsersApiTokensTabComponent } from './tabs/dot-users-api-tokens-tab/dot-users-api-tokens-tab.component';
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
 */
export type DotUsersDialogResult =
    | { action: 'save'; mode: 'create' | 'update'; payload: DotUserFormPayload }
    | { action: 'delete'; userId: string; replacementUserId?: string };

/**
 * Additional-info keys the Profile tab surfaces as first-class fields.
 * These are conventional dotCMS keys — the backend stores them inside
 * `User.additionalInfo` (a free-form map) and returns them unchanged.
 */
const ADDITIONAL_INFO_KEYS = ['prefix', 'suffix', 'title', 'company', 'website'] as const;

/**
 * Create / Edit User dialog. Hosts the four-tab experience (Profile,
 * Roles, Permissions, API Tokens) inside the shared PrimeNG dynamic
 * dialog frame. Owns the top-level reactive form, mode detection
 * (create vs edit), and orchestrates each tab as a standalone
 * presentational sub-component.
 *
 * Scope for issue #36720 — Profile + API Tokens tabs are real; Roles
 * and Permissions render "Coming soon" placeholders and are delivered
 * by #36718 and #36719.
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
        DotUsersProfileTabComponent,
        DotUsersApiTokensTabComponent
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
            this.disableAccessSection();
        } else {
            this.enableCreatePasswordValidators();
        }
    }

    protected close(): void {
        this.dialogRef.close();
    }

    protected save(): void {
        this.form.markAllAsTouched();
        if (this.form.invalid) {
            return;
        }

        const payload = this.buildPayload();
        const result: DotUsersDialogResult = {
            action: 'save',
            mode: this.isEdit ? 'update' : 'create',
            payload
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
                canLogin: item.hasConsoleAccess ?? false,
                showGettingStarted: false
            }
        });
    }

    /**
     * Fetches the full user detail — the list row does not carry
     * `additionalInfo` (prefix/suffix/title/company/website), so we
     * re-fetch to hydrate those fields. Access section stays disabled,
     * so no need to reconcile its values against roles yet.
     */
    private loadUserDetail(userId: string): void {
        this.isLoading.set(true);
        this.usersService
            .getUser(userId)
            .pipe(
                take(1),
                finalize(() => this.isLoading.set(false)),
                takeUntilDestroyed(this.destroyRef)
            )
            .subscribe({
                next: (user) => {
                    const additionalInfo = user.additionalInfo ?? {};
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
                            cmsAdmin: user.admin ?? false,
                            backend: user.backendUser ?? false,
                            frontend: user.frontendUser ?? false,
                            canLogin: user.hasConsoleAccess ?? false,
                            showGettingStarted: false
                        }
                    });
                    this.form.markAsPristine();
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
     * Access toggles map to backend roles (CMS Administrator,
     * DOTCMS_BACK_END_USER, DOTCMS_FRONT_END_USER, hasConsoleAccess).
     * Path A does not persist those, so the section is disabled at load
     * to keep the values informational only.
     */
    private disableAccessSection(): void {
        this.form.controls.access.disable({ emitEvent: false });
    }

    /**
     * Builds the backend {@link DotUserFormPayload} from the current
     * form value. Password and additionalInfo are omitted when empty so
     * the backend keeps the existing values.
     *
     * Roles are intentionally NOT sent — the backend interprets a
     * missing `roles` field as "leave role membership untouched"
     * (UserResource#processRoles), which is exactly what Path A needs.
     */
    private buildPayload(): DotUserFormPayload {
        const raw = this.form.getRawValue();
        const account = raw.account;
        const additionalInfoValue = raw.additionalInfo;

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

        return payload;
    }
}
