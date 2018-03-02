import { DotMessageService } from '../dot-messages-service';
import { Injectable } from '@angular/core';
import { DotDialog } from '../../../shared/models/dot-confirmation/dot-confirmation.model';
import { ConfirmationService } from 'primeng/primeng';

/**
 * Handle global confirmation and alert dialog component
 * @export
 * @class DotDialogService
 */

@Injectable()
export class DotDialogService {
    alertModel: DotDialog = null;
    confirmModel: DotDialog = null;

    constructor(public confirmationService: ConfirmationService, private dotMessageService: DotMessageService) {
        this.dotMessageService.getMessages(['dot.common.dialog.accept', 'dot.common.dialog.reject']).subscribe();
    }

    /**
     * Confirm wrapper method of ConfirmService
     * Add both accept and reject labels into confirmation object
     * @param {DotDialog} dialogModel
     * @memberof DotDialogService
     */
    confirm(dialogModel: DotDialog): void {
        dialogModel.footerLabel = {
            accept: this.dotMessageService.get('dot.common.dialog.accept'),
            reject: this.dotMessageService.get('dot.common.dialog.reject'),
            ...dialogModel.footerLabel
        };

        this.confirmModel = dialogModel;
        setTimeout(() => {
            this.confirmationService.confirm(dialogModel);
        }, 0);
    }

    /**
     * Confirm wrapper method of ConfirmService
     * Add both accept and reject labels into confirmation object
     * @param {DotDialog} confirmation
     * @memberof DotDialogService
     */
    alert(dialogModel: DotDialog): void {
        dialogModel.footerLabel = {
            accept: this.dotMessageService.get('dot.common.dialog.accept'),
            ...dialogModel.footerLabel
        };

        this.alertModel = dialogModel;
    }

    /**
     * clear alert dialog object
     *
     * @memberof DotDialogService
     */
    clearAlert(): void {
        this.alertModel = null;
    }

    /**
     * clear confirm dialog object
     *
     * @memberof DotDialogService
     */
    clearConfirm(): void {
        this.confirmModel = null;
    }
}
