import _ from 'lodash';
import {Component, ViewEncapsulation} from '@angular/core';
import {MessageService} from '../../../../api/services/messages-service';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'base'
})

export class BaseComponent {
    public messageMapSubscription;
    public i18nMessages = {};

    constructor(i18nKeys: string[], private messageService: MessageService) {
        if (messageService !== null) {
            this.messageMapSubscription = this.messageService.getMessages(i18nKeys).subscribe(res => {
                this.i18nMessages = _.pick(res, i18nKeys);
            });
        }
    }

    ngOnDestroy(): void {
        this.messageMapSubscription.unsubscribe();
    }
}