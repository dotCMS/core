import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

import { DotUserListItem } from '../../../services/dot-users.service';
import { generateSecurePassword } from '../../dot-users-form.model';

interface AccessRow {
    key: 'cmsAdmin' | 'backend' | 'frontend' | 'showGettingStarted';
    titleKey: string;
    descriptionKey: string;
}

const ACCESS_ROWS: AccessRow[] = [
    {
        key: 'cmsAdmin',
        titleKey: 'users.dialog.toggle.cms-admin.title',
        descriptionKey: 'users.dialog.toggle.cms-admin.description'
    },
    {
        key: 'backend',
        titleKey: 'users.dialog.toggle.backend.title',
        descriptionKey: 'users.dialog.toggle.backend.description'
    },
    {
        key: 'frontend',
        titleKey: 'users.dialog.toggle.frontend.title',
        descriptionKey: 'users.dialog.toggle.frontend.description'
    },
    {
        key: 'showGettingStarted',
        titleKey: 'users.dialog.toggle.show-getting-started.title',
        descriptionKey: 'users.dialog.toggle.show-getting-started.description'
    }
];

/**
 * Profile tab of the Create/Edit User dialog. Owns the visuals — the
 * shell component owns the `FormGroup` reference and dispatches saves,
 * so this component stays purely presentational and testable in
 * isolation.
 */
@Component({
    selector: 'dot-users-profile-tab',
    standalone: true,
    imports: [
        DatePipe,
        ReactiveFormsModule,
        ButtonModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        PasswordModule,
        ToggleSwitchModule,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    templateUrl: './dot-users-profile-tab.component.html',
    styleUrl: './dot-users-profile-tab.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col gap-6 block' }
})
export class DotUsersProfileTabComponent {
    readonly form = input.required<FormGroup>();
    readonly isEdit = input<boolean>(false);
    readonly user = input<DotUserListItem | null>(null);

    readonly deleteRequested = output<void>();

    /**
     * The confirmation dialog for Delete User is rendered by the shell,
     * but the section itself lives inside the Profile tab. Emit an event
     * up so the shell can decide how to prompt.
     */
    protected onDeleteClick(): void {
        this.deleteRequested.emit();
    }

    protected readonly accessRows = ACCESS_ROWS;

    protected readonly showPassword = signal(false);

    protected readonly accountGroup = computed(() => this.form().get('account') as FormGroup);

    protected readonly additionalInfoGroup = computed(
        () => this.form().get('additionalInfo') as FormGroup
    );

    protected readonly accessGroup = computed(() => this.form().get('access') as FormGroup);

    protected onGeneratePassword(): void {
        const password = generateSecurePassword();
        const account = this.accountGroup();
        account.patchValue({ password, confirmPassword: password });
        account.get('password')?.markAsDirty();
        account.get('confirmPassword')?.markAsDirty();
    }

    /**
     * Whether a control (relative to `accountGroup`) is in an error
     * state we want to visualize on the input. PrimeNG's `[invalid]`
     * input drives the red-outline treatment; we base it on
     * `touched || dirty` so Save's `markAllAsTouched()` triggers it
     * even for fields the user never focused.
     */
    protected isAccountFieldInvalid(controlName: string): boolean {
        const control = this.accountGroup().get(controlName);

        return !!control && control.invalid && (control.touched || control.dirty);
    }

    /**
     * Cross-field mismatch on the account group counts as an error on
     * the confirm-password field visually — same treatment as the
     * required error but the source is the group-level validator.
     */
    protected isConfirmPasswordInvalid(): boolean {
        const group = this.accountGroup();
        const confirm = group.get('confirmPassword');
        if (!confirm) {
            return false;
        }
        const touched = confirm.touched || confirm.dirty;

        return touched && (confirm.invalid || !!group.errors?.['passwordMismatch']);
    }
}
