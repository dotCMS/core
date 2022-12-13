import { DotAlertConfirmService } from '@dotcms/data-access';
import { Component, ViewChild, OnInit, ElementRef, OnDestroy } from '@angular/core';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
    selector: 'dot-alert-confirm',
    templateUrl: './dot-alert-confirm.html'
})
export class DotAlertConfirmComponent implements OnInit, OnDestroy {
    @ViewChild('cd') cd: ConfirmDialog;
    @ViewChild('confirmBtn') confirmBtn: ElementRef;
    @ViewChild('acceptBtn') acceptBtn: ElementRef;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(public dotAlertConfirmService: DotAlertConfirmService) {}

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
