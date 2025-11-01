import { Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { takeUntil } from 'rxjs/operators';

import { DotGenerateSecurePasswordService, DotMessageService } from '@dotcms/data-access';
import { DotDialogActions } from '@dotcms/dotcms-models';
import { DotClipboardUtil, DotDialogComponent, DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-generate-secure-password',
    templateUrl: './dot-generate-secure-password.component.html',
    styleUrls: ['./dot-generate-secure-password.component.scss'],
    imports: [CommonModule, ButtonModule, DotDialogComponent, DotMessagePipe],
    providers: [DotClipboardUtil]
})
export class DotGenerateSecurePasswordComponent implements OnInit, OnDestroy {
    private dotClipboardUtil = inject(DotClipboardUtil);
    private dotMessageService = inject(DotMessageService);
    private dotGenerateSecurePassword = inject(DotGenerateSecurePasswordService);

    copyBtnLabel: string;
    dialogActions: DotDialogActions;
    dialogShow = false;
    revealBtnLabel: string;
    typeInput = 'password';
    value: string;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnInit() {
        this.setUILabels();
        this.createDialogActions();
        this.dotGenerateSecurePassword.showDialog$
            .pipe(takeUntil(this.destroy$))
            .subscribe(({ password }) => {
                this.value = password;
                this.dialogShow = true;
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Close the dialog and clear the data.
     * @memberof DotGenerateSecurePasswordComponent
     */
    close(): void {
        this.dialogShow = false;
        this.typeInput = 'password';
        this.revealBtnLabel = this.dotMessageService.get('generate.secure.password.reveal');
        this.value = '';
    }

    /**
     * Copy to clipboard the value of the password.
     * @memberof DotGenerateSecurePasswordComponent
     */
    copyToClipboard(): void {
        this.dotClipboardUtil.copy(this.value);

        this.copyBtnLabel = this.dotMessageService.get('Copied');
        setTimeout(() => {
            this.copyBtnLabel = this.dotMessageService.get('Copy');
        }, 2000);
    }

    /**
     * Shows/Hide the value of the password.
     * @param {MouseEvent} $event
     * @memberof DotGenerateSecurePasswordComponent
     */
    revealPassword($event: MouseEvent) {
        $event.stopPropagation();
        $event.preventDefault();
        this.typeInput = this.typeInput === 'password' ? 'text' : 'password';
        this.revealBtnLabel =
            this.typeInput === 'password'
                ? this.dotMessageService.get('generate.secure.password.reveal')
                : this.dotMessageService.get('hide');
    }

    private setUILabels() {
        this.copyBtnLabel = this.dotMessageService.get('Copy');
        this.revealBtnLabel = this.dotMessageService.get('generate.secure.password.reveal');
    }

    private createDialogActions() {
        this.dialogActions = {
            cancel: {
                action: () => {
                    this.close();
                },
                label: this.dotMessageService.get('Close')
            }
        };
    }
}
