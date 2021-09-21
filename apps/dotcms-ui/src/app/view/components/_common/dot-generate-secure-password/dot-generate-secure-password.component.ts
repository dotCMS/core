import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { DotDialogActions } from '../../dot-dialog/dot-dialog.component';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotGenerateSecurePasswordService } from '@services/dot-generate-secure-password/dot-generate-secure-password.service';
import { DotClipboardUtil } from '@dotcms/app/api/util/clipboard/ClipboardUtil';

@Component({
    selector: 'dot-generate-secure-password',
    templateUrl: './dot-generate-secure-password.component.html',
    styleUrls: ['./dot-generate-secure-password.component.scss']
})
export class DotGenerateSecurePasswordComponent implements OnInit, OnDestroy {
    dialogActions: DotDialogActions;
    dialogShow = false;
    typeInput = 'password';
    value: string;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotClipboardUtil: DotClipboardUtil,
        private dotMessageService: DotMessageService,
        private dotGenerateSecurePassword: DotGenerateSecurePasswordService
    ) {}

    ngOnInit() {
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
        this.value = '';
    }

    /**
     * Copy to clipboard the value of the password.
     * @memberof DotGenerateSecurePasswordComponent
     */
    copyToClipboard(): void {
        this.dotClipboardUtil.copy(this.value);
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
