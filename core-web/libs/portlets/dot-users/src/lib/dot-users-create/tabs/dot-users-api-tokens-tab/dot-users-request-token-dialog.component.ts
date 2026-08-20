import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';

import { take } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

import { DotApiTokenCreateResult, DotUsersService } from '../../../services/dot-users.service';

interface RequestTokenDialogData {
    userId: string;
}

/**
 * Sub-dialog opened from the API Tokens tab. Collects Label + Expires
 * Date + Allow Network, POSTs to `/api/v1/apitoken`, and closes with
 * the created token plus the raw JWT so the parent tab can reveal it
 * once (design choice: the JWT never re-appears after this dialog).
 */
@Component({
    selector: 'dot-users-request-token-dialog',
    standalone: true,
    imports: [
        ReactiveFormsModule,
        ButtonModule,
        InputTextModule,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    templateUrl: './dot-users-request-token-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUsersRequestTokenDialogComponent {
    private readonly dialogRef = inject(DynamicDialogRef);
    private readonly config =
        inject<DynamicDialogConfig<RequestTokenDialogData>>(DynamicDialogConfig);
    private readonly fb = inject(FormBuilder);
    private readonly usersService = inject(DotUsersService);
    private readonly httpErrorManager = inject(DotHttpErrorManagerService);

    private readonly userId = this.config.data?.userId ?? '';

    protected readonly form = this.fb.nonNullable.group({
        label: ['', [Validators.required]],
        expires: [this.defaultExpiryDate(), [Validators.required]],
        network: ['0.0.0.0/0']
    });

    protected readonly submitting = signal(false);

    /**
     * Save is only enabled once we have a userId (parent hydrated the
     * dialog) and while a request is not already in flight. The
     * form-level `disabled` also flips based on validity via
     * `markAllAsTouched` + PrimeNG's `[invalid]` styling.
     */
    protected readonly canSubmit = computed(() => !this.submitting() && !!this.userId);

    protected close(): void {
        this.dialogRef.close();
    }

    protected submit(): void {
        this.form.markAllAsTouched();
        if (this.form.invalid || !this.canSubmit()) {
            return;
        }

        const value = this.form.getRawValue();
        const expirationSeconds = this.expirationSecondsFrom(value.expires);
        if (expirationSeconds <= 0) {
            // Guard against the "past" case even though the date input's
            // `min` should keep this out of reach.
            this.form.controls.expires.setErrors({ past: true });

            return;
        }

        this.submitting.set(true);
        this.usersService
            .createApiToken({
                userId: this.userId,
                expirationSeconds,
                network: value.network.trim() || undefined,
                claims: value.label.trim() ? { label: value.label.trim() } : undefined
            })
            .pipe(take(1))
            .subscribe({
                next: (result: DotApiTokenCreateResult) => {
                    this.submitting.set(false);
                    this.dialogRef.close(result);
                },
                error: (error) => {
                    this.submitting.set(false);
                    this.httpErrorManager.handle(error);
                }
            });
    }

    private expirationSecondsFrom(expiresIsoDate: string): number {
        const target = new Date(expiresIsoDate).getTime();
        if (Number.isNaN(target)) {
            return 0;
        }

        return Math.ceil((target - Date.now()) / 1000);
    }

    private defaultExpiryDate(): string {
        const date = new Date();
        date.setFullYear(date.getFullYear() + 3);

        return date.toISOString().slice(0, 10);
    }
}
