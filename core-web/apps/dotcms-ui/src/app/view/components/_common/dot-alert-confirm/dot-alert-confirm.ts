import { Subject } from 'rxjs';

import {
    afterNextRender,
    Component,
    ElementRef,
    inject,
    Injector,
    OnDestroy,
    OnInit,
    ViewChild
} from '@angular/core';

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
    private injector = inject(Injector);

    @ViewChild('cd') cd: ConfirmDialog;
    @ViewChild('confirmBtn') confirmBtn: ElementRef;
    @ViewChild('acceptBtn') acceptBtn: ElementRef;

    private destroy$ = new Subject<boolean>();

    ngOnInit(): void {
        this.dotAlertConfirmService.confirmDialogOpened$
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                const btn = this.confirmBtn || this.acceptBtn;
                if (btn?.nativeElement) {
                    afterNextRender(() => btn.nativeElement.focus(), { injector: this.injector });
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
        const model = this.dotAlertConfirmService.confirmModel();
        if (action === 'accept') {
            if (model?.accept) {
                model.accept();
            }
            this.confirmationService.onAccept();
        } else {
            if (model?.reject) {
                model.reject();
            }
            this.confirmationService.close();
        }
        this.dotAlertConfirmService.clearConfirm();
    }
}
