import { DotAlertConfirmService } from '@services/dot-alert-confirm/dot-alert-confirm.service';
import { Component, ViewChild, OnInit, ElementRef, OnDestroy } from '@angular/core';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
    selector: 'dot-alert-confirm',
    templateUrl: './dot-alert-confirm.html',
    styleUrls: ['./dot-alert-confirm.scss']
})
export class DotAlertConfirmComponent implements OnInit, OnDestroy {
    @ViewChild('cd') cd: ConfirmDialog;
    @ViewChild('confirmBtn') confirmBtn: ElementRef;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        public dotAlertConfirmService: DotAlertConfirmService
    ) {}

    ngOnInit(): void {
        this.dotAlertConfirmService.confirmDialogOpened$
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                setTimeout(() => {
                    this.confirmBtn.nativeElement.focus();
                });
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
