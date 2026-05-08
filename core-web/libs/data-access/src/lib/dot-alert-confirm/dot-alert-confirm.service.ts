import { Observable, Subject } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { ConfirmationService } from 'primeng/api';

import { DotAlertConfirm } from '@dotcms/dotcms-models';

import { DotMessageService } from '../dot-messages/dot-messages.service';

/**
 * Handle global confirmation and alert dialog component
 * @export
 * @class DotAlertConfirmService
 */

@Injectable()
export class DotAlertConfirmService {
    confirmationService = inject(ConfirmationService);
    private dotMessageService = inject(DotMessageService);

    alertModel: DotAlertConfirm | null = null;
    confirmModel: DotAlertConfirm | null = null;
    private _confirmDialogOpened$ = new Subject<boolean>();

    /**
     * Get the confirmDialogOpened notification as an Observable
     * @returns Observable<boolean>
     */
    get confirmDialogOpened$(): Observable<boolean> {
        return this._confirmDialogOpened$.asObservable();
    }

    /**
     * Confirm wrapper method of ConfirmService
     * Add both accept and reject labels into confirmation object
     *
     * @param DotAlertConfirm dialogModel
     * @memberof DotAlertConfirmService
     */
    confirm(dialogModel: DotAlertConfirm): void {
        dialogModel.footerLabel = {
            accept: this.dotMessageService.get('dot.common.dialog.accept'),
            reject: this.dotMessageService.get('dot.common.dialog.reject'),
            ...dialogModel.footerLabel
        };

        this.confirmModel = dialogModel;
        setTimeout(() => {
            this.confirmationService.confirm(dialogModel);
            this._confirmDialogOpened$.next(true);
        }, 0);
    }

    /**
     * Confirm wrapper method of ConfirmService
     * Add both accept and reject labels into confirmation object
     *
     * @param DotAlertConfirm confirmation
     * @memberof DotAlertConfirmService
     */
    alert(dialogModel: DotAlertConfirm): void {
        dialogModel.footerLabel = {
            accept: this.dotMessageService.get('dot.common.dialog.accept'),
            ...dialogModel.footerLabel
        };

        this.alertModel = dialogModel;
        setTimeout(() => {
            this._confirmDialogOpened$.next(true);
        }, 0);
    }

    /**
     * Call the alert accept action and clear the model
     *
     * @param Event $event
     * @memberof DotAlertConfirmService
     */
    alertAccept($event?: Event): void {
        if (this.alertModel?.accept) {
            this.alertModel.accept($event);
        }

        this.alertModel = null;
    }

    /**
     * Call the alert reject action and clear the model
     *
     * @memberof DotAlertConfirmService
     */
    alertReject($event: Event): void {
        if (this.alertModel?.reject) {
            this.alertModel.reject($event);
        }

        this.alertModel = null;
    }

    /**
     * clear confirm dialog object
     *
     * @memberof DotAlertConfirmService
     */
    clearConfirm(): void {
        this.confirmModel = null;
    }
}
