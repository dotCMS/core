import { Injectable, EventEmitter } from '@angular/core';
import { FooterLabels } from './../../shared/models/dot-confirmation/footer-labels.model';
import { DotConfirmation } from './../../shared/models/dot-confirmation/dot-confirmation.model';
import { BehaviorSubject } from 'rxjs';
import { MessageService } from './messages-service';
import { ConfirmationService } from 'primeng/primeng';

@Injectable()
export class DotConfirmationService {
    labels: BehaviorSubject<FooterLabels> = new BehaviorSubject({});
    public i18nMessages = {};
    public i18nKeys: string[] = [
        'contenttypes.action.yes',
        'contenttypes.action.no'
    ];

    constructor(public confirmationService: ConfirmationService) {
    }

    // TODO: Import MessageService - Add message keys
    // (Not working right now since inyecting MessageService produces errors)
    confirm(confirmation: DotConfirmation): void {
        this.labels.next({
            acceptLabel: confirmation.footerLabel.acceptLabel || 'Yes',
            rejectLabel: confirmation.footerLabel.rejectLabel || 'No'
        });
        this.confirmationService.confirm(confirmation);
    }

}

