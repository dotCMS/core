import { Subject } from 'rxjs';

import { Component, ElementRef, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialog, ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';

import { takeUntil } from 'rxjs/operators';

import { DotAlertConfirmService } from '@dotcms/data-access';

@Component({
    selector: 'dot-alert-confirm',
    templateUrl: './dot-alert-confirm.html',
    imports: [ConfirmDialogModule, DialogModule, ButtonModule]
})
export class DotAlertConfirmComponent implements OnInit, OnDestroy {
    dotAlertConfirmService = inject(DotAlertConfirmService);
    private confirmationService = inject(ConfirmationService);

    @ViewChild('cd') cd: ConfirmDialog;
    @ViewChild('confirmBtn') confirmBtn: ElementRef;
    @ViewChild('acceptBtn') acceptBtn: ElementRef;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnInit(): void {
        this.dotAlertConfirmService.confirmDialogOpened$
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                const btn = this.confirmBtn || this.acceptBtn;
                if (btn?.nativeElement) {
                    btn.nativeElement.focus();
                }
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Handle confirmation dialog action button click
     *
     * @param string action
     * @memberof DotAlertConfirmComponent
     */
    onClickConfirm(action: string): void {
        if (action === 'accept') {
            // Call the accept callback if it exists
            if (this.dotAlertConfirmService.confirmModel?.accept) {
                this.dotAlertConfirmService.confirmModel.accept();
            }
            this.confirmationService.onAccept();
        } else {
            // Call the reject callback if it exists
            if (this.dotAlertConfirmService.confirmModel?.reject) {
                this.dotAlertConfirmService.confirmModel.reject();
            }
            this.confirmationService.close();
        }
        this.dotAlertConfirmService.clearConfirm();
    }
}
