import { Component, computed, inject, signal } from '@angular/core';
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
 * the created token plus a first JWT for the parent tab's reveal
 * surface. The parent can re-mint the JWT later via `getApiTokenJwt`.
 */
@Component({
    selector: 'dot-users-request-token-dialog',
    imports: [
        ReactiveFormsModule,
        ButtonModule,
        InputTextModule,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    templateUrl: './dot-users-request-token-dialog.component.html'
})
export class DotUsersRequestTokenDialogComponent {
    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #config = inject<DynamicDialogConfig<RequestTokenDialogData>>(DynamicDialogConfig);
    readonly #fb = inject(FormBuilder);
    readonly #usersService = inject(DotUsersService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);

    readonly #userId = this.#config.data?.userId ?? '';

    protected readonly form = this.#fb.nonNullable.group({
        label: ['', [Validators.required]],
        expires: [this.defaultExpiryDate(), [Validators.required]],
        network: ['0.0.0.0/0']
    });

    protected readonly $submitting = signal(false);

    /**
     * Save is only enabled once we have a userId (parent hydrated the
     * dialog) and while a request is not already in flight. The
     * form-level `disabled` also flips based on validity via
     * `markAllAsTouched` + PrimeNG's `[invalid]` styling.
     */
    protected readonly $canSubmit = computed(() => !this.$submitting() && !!this.#userId);

    protected close(): void {
        this.#dialogRef.close();
    }

    protected submit(): void {
        this.form.markAllAsTouched();
        if (this.form.invalid || !this.$canSubmit()) {
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

        this.$submitting.set(true);
        this.#usersService
            .createApiToken({
                userId: this.#userId,
                expirationSeconds,
                network: value.network.trim() || undefined,
                claims: value.label.trim() ? { label: value.label.trim() } : undefined
            })
            .pipe(take(1))
            .subscribe({
                next: (result: DotApiTokenCreateResult) => {
                    this.$submitting.set(false);
                    this.#dialogRef.close(result);
                },
                error: (error) => {
                    this.$submitting.set(false);
                    this.#httpErrorManager.handle(error);
                }
            });
    }

    private expirationSecondsFrom(expiresIsoDate: string): number {
        // Interpret the YYYY-MM-DD string as LOCAL midnight — not UTC
        // midnight, which is what `new Date('YYYY-MM-DD')` returns.
        // West of UTC that shifts the expiration back into the
        // previous evening, and rejects "today" as past.
        const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(expiresIsoDate);
        if (!match) {
            return 0;
        }

        const [, year, month, day] = match;
        const target = new Date(Number(year), Number(month) - 1, Number(day)).getTime();
        if (Number.isNaN(target)) {
            return 0;
        }

        return Math.ceil((target - Date.now()) / 1000);
    }

    private defaultExpiryDate(): string {
        const date = new Date();
        date.setFullYear(date.getFullYear() + 3);
        // Use local Y/M/D so the value matches what the user sees on
        // the date input in their timezone.
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');

        return `${year}-${month}-${day}`;
    }
}
