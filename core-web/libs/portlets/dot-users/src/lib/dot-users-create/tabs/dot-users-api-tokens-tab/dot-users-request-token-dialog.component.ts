import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';

import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

export interface DotUsersRequestTokenPayload {
    label: string;
    expires: string;
    network: string;
    requestedBy: string;
}

/**
 * Sub-dialog opened from the API Tokens tab. Collects the three
 * fields the design surfaces (Label, Expires Date, Allow Network)
 * and closes with the payload so the parent tab can prepend the
 * new token to its list.
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
    private readonly fb = inject(FormBuilder);

    protected readonly form = this.fb.nonNullable.group({
        label: ['', [Validators.required]],
        expires: [this.defaultExpiryDate(), [Validators.required]],
        network: ['0.0.0.0/0']
    });

    protected close(): void {
        this.dialogRef.close();
    }

    protected submit(): void {
        this.form.markAllAsTouched();
        if (this.form.invalid) {
            return;
        }

        const value = this.form.getRawValue();
        const payload: DotUsersRequestTokenPayload = {
            label: value.label.trim(),
            expires: value.expires,
            network: value.network.trim(),
            requestedBy: 'dotcms.admin' // TODO: replace with current user id when endpoint lands
        };

        this.dialogRef.close(payload);
    }

    private defaultExpiryDate(): string {
        const date = new Date();
        date.setFullYear(date.getFullYear() + 3);

        return date.toISOString().slice(0, 10);
    }
}
