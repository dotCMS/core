import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';

import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';

import { DotMessageDisplayService } from '@dotcms/data-access';
import { DotMessage } from '@dotcms/dotcms-models';
import { DotIconComponent } from '@dotcms/ui';

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
    templateUrl: 'dot-message-display.component.html',
    imports: [CommonModule, ToastModule, DotIconComponent]
})
export class DotMessageDisplayComponent implements OnInit, OnDestroy {
    private dotMessageDisplayService = inject(DotMessageDisplayService);
    private messageService = inject(MessageService);

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
