import { Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';

import { ConfirmDialog, ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';

import { takeUntil } from 'rxjs/operators';

import { DotAlertConfirmService } from '@dotcms/data-access';

@Component({
    selector: 'dot-alert-confirm',
    templateUrl: './dot-alert-confirm.html',
    imports: [CommonModule, ConfirmDialogModule, DialogModule]
})
export class DotAlertConfirmComponent implements OnInit, OnDestroy {
    dotAlertConfirmService = inject(DotAlertConfirmService);

    @ViewChild('cd') cd: ConfirmDialog;
    @ViewChild('confirmBtn') confirmBtn: ElementRef;
    @ViewChild('acceptBtn') acceptBtn: ElementRef;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnInit(): void {
        this.dotAlertConfirmService.confirmDialogOpened$
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                const btn = this.confirmBtn || this.acceptBtn;
                btn.nativeElement.focus();
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
        action === 'accept' ? this.cd.accept() : this.cd.reject();
        this.dotAlertConfirmService.clearConfirm();
    }
}
