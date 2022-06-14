import { Component, OnInit, OnDestroy } from '@angular/core';
import { DotMessageDisplayService } from './services';
import { MessageService } from 'primeng/api';
import { DotMessage } from './model/dot-message.model';

/**
 *Show message send from the Backend
 *
 * @export
 * @class DotMessageDisplayComponent
 * @implements {OnInit}
 * @implements {OnDestroy}
 */
@Component({
    providers: [MessageService],
    selector: 'dot-message-display',
    styleUrls: ['dot-message-display.component.scss'],
    templateUrl: 'dot-message-display.component.html'
})
export class DotMessageDisplayComponent implements OnInit, OnDestroy {
    constructor(
        private dotMessageDisplayService: DotMessageDisplayService,
        private messageService: MessageService
    ) {}

    ngOnInit() {
        this.dotMessageDisplayService.messages().subscribe((dotMessage: DotMessage) => {
            this.messageService.add({
                life: dotMessage.life,
                detail: dotMessage.message,
                severity: dotMessage.severity.toLowerCase()
            });
        });
    }

    ngOnDestroy(): void {
        this.dotMessageDisplayService.unsubscribe();
    }
}
