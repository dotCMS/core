
import {Component, ViewEncapsulation} from '@angular/core';
import {MessageService} from '../../../../api/services/messages-service';
import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs/Subject';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'base'
})

export class BaseComponent {
    public messageMapSubscription;
    public i18nMessages = {};

    constructor(i18nKeys: string[], public messageService: MessageService) {
        if (messageService !== null) {
            this.messageMapSubscription = this.messageService.getMessages(i18nKeys).subscribe(res => {
                this.i18nMessages = res;
                this.onMessage();
            });
        }
    }

    ngOnDestroy(): void {
        this.messageMapSubscription.unsubscribe();
    }

    /**
     * Call when the messages are receive
     * @memberOf BaseComponent
     */
    onMessage(): void {

    }
}