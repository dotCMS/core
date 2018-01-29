import { Injectable } from '@angular/core';
import { FooterLabels } from './../../../shared/models/dot-confirmation/footer-labels.model';
import { DotConfirmation } from './../../../shared/models/dot-confirmation/dot-confirmation.model';
import { Subject } from 'rxjs/Subject';
import { DotMessageService } from '../dot-messages-service';
import { ConfirmationService } from 'primeng/primeng';

/**
 * Provides a wrapper of ConfirmationService
 * @export
 * @class DotConfirmationService
 */

@Injectable()
export class DotConfirmationService {
    showRejectButton: boolean;
    labels: Subject<FooterLabels> = new Subject();
    public i18nMessages = {};
    public i18nKeys: string[] = [
        'contenttypes.action.yes',
        'contenttypes.action.no'
    ];

    constructor(public confirmationService: ConfirmationService) {
    }

    /**
     * Confirm wrapper method of ConfirmService
     * Add both accept and reject labels into confirmation object
     * @param {DotConfirmation} confirmation
     * @memberof DotConfirmationService
     */
    // TODO: Import DotMessageService - Add message keys
    // (Not working right now since inyecting DotMessageService produces errors)
    confirm(confirmation: DotConfirmation): void {
        this.showRejectButton = true;
        this.labels.next({
            acceptLabel: confirmation.footerLabel.acceptLabel || 'Yes',
            rejectLabel: confirmation.footerLabel.rejectLabel || 'No'
        });

        this.confirmationService.confirm(confirmation);
    }

    /**
     * Confirm wrapper method of ConfirmService
     * Add both accept and reject labels into confirmation object
     * @param {DotConfirmation} confirmation
     * @memberof DotConfirmationService
     */
    alert(confirmationParameter: DotConfirmation): void {
        this.labels.next({
            acceptLabel: confirmationParameter.footerLabel.acceptLabel || 'Yes'
        });

        const confirmation  = Object.assign({}, confirmationParameter);
        confirmation.accept = () => {

        };

        this.showRejectButton = false;
        this.confirmationService.confirm(confirmation);
    }
}

