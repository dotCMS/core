import { EMPTY } from 'rxjs';

import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';

import { catchError, finalize } from 'rxjs/operators';

import { DotCompanyConfigurationService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { isValidSender } from '../../store/dot-configuration.mappers';

export interface DotConfigurationTestMailDialogData {
    /** Sender currently in the form, in `Name <address>` form. */
    sender: string;
}

/**
 * Sends a test email from a sender address to the logged-in user. The sender can be edited
 * before sending, as in the previous screen; editing it here does not change the saved value.
 *
 * The dialog sends the request itself so a rejected address keeps the dialog open with what the
 * user typed. It closes with the sender once the server has queued the email.
 */
@Component({
    selector: 'dot-configuration-test-mail-dialog',
    imports: [FormsModule, ButtonModule, InputTextModule, DotMessagePipe],
    templateUrl: './dot-configuration-test-mail-dialog.component.html',
    styleUrls: ['./dot-configuration-test-mail-dialog.component.scss']
})
export class DotConfigurationTestMailDialogComponent {
    readonly #ref = inject(DynamicDialogRef);
    readonly #config = inject(DynamicDialogConfig<DotConfigurationTestMailDialogData>);
    readonly #service = inject(DotCompanyConfigurationService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #destroyRef = inject(DestroyRef);

    protected readonly $sender = signal(this.#config.data?.sender ?? '');
    protected readonly $sending = signal(false);
    protected readonly $invalid = computed(() => !isValidSender(this.$sender()));

    onSenderChange(sender: string): void {
        this.$sender.set(sender);
    }

    send(): void {
        if (this.$invalid() || this.$sending()) {
            return;
        }

        const sender = this.$sender().trim();

        this.$sending.set(true);
        this.#service
            .sendTestEmail(sender)
            .pipe(
                catchError((error) => {
                    this.#httpErrorManager.handle(error);

                    return EMPTY;
                }),
                finalize(() => this.$sending.set(false)),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe(() => this.#ref.close(sender));
    }

    cancel(): void {
        this.#ref.close();
    }
}
