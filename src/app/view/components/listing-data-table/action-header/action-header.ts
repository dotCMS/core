import { Component, Input, SimpleChanges, ViewEncapsulation, OnChanges } from '@angular/core';

import { ConfirmationService } from 'primeng/primeng';

import { BaseComponent } from '../../_common/_base/base-component';
import { MessageService } from '../../../../api/services/messages-service';
import { ActionHeaderOptions, ButtonAction } from '../../../../shared/models/action-header';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'action-header',
    styleUrls: ['./action-header.scss'],
    templateUrl: 'action-header.html'
})

export class ActionHeaderComponent extends BaseComponent implements OnChanges {
    @Input() selectedItems = [];
    @Input() options: ActionHeaderOptions;
    public dynamicOverflow = 'visible';

    constructor(messageService: MessageService, private confirmationService: ConfirmationService) {
        super(['selected'], messageService);
    }

    ngOnChanges(changes: SimpleChanges): any {
        if (changes.selected) {
            this.hideDinamycOverflow();
        }

        if (changes.options && changes.options.currentValue && changes.options.currentValue.secondary) {
            this.setCommandWrapper(changes.options.currentValue.secondary);
        }
    }

    private setCommandWrapper(options: ButtonAction[]): void {
        options.forEach(actionButton => {
            actionButton.model
                .filter(model => model.deleteOptions)
                .forEach(model => {
                    if (typeof model.command === 'function') {
                        const callback = model.command ;
                        model.command = ($event) => {
                            const originalEvent = $event;

                            this.confirmationService.confirm({
                                accept: () => {
                                    callback(originalEvent);
                                },
                                header: model.deleteOptions.confirmHeader,
                                message: model.deleteOptions.confirmMessage,
                            });
                        };
                    }
                });
        });
    }

    private hideDinamycOverflow(): void {
        this.dynamicOverflow = '';
        if (this.selectedItems.length) {
            setTimeout(() => {
                this.dynamicOverflow = 'visible';
            }, 300);
        }
    }
}
