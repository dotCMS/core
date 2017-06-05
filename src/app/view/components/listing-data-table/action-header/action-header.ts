import { BaseComponent } from '../../_common/_base/base-component';
import { Component, Input, SimpleChanges, ViewEncapsulation } from '@angular/core';
import { ConfirmationService } from 'primeng/primeng';
import { MessageService } from '../../../../api/services/messages-service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'action-header',
    styles: [require('./action-header.scss')],
    templateUrl: 'action-header.html'
})

export class ActionHeaderComponent extends BaseComponent {

    public dynamicOverflow = 'visible';

    @Input() selected = false;
    @Input() selectedItems = [];
    @Input() actionButtonItems: ButtonAction[];
    @Input() primaryCommand: Function;

    constructor(messageService: MessageService, private confirmationService: ConfirmationService) {
        super(['selected'], messageService);
    }

    private ngOnChanges(changes: SimpleChanges): any {
        if (changes.selected) {
            this.hideDinamycOverflow();
        }

        if (changes.actionButtonItems) {
            this.setCommandWrapper(changes.actionButtonItems.currentValue);
        }
    }

    private setCommandWrapper(actionButtonItems: ButtonAction[]): void {
        actionButtonItems.forEach(actionButton => {
            actionButton.model
                .filter(model => model.deleteOptions)
                .forEach(model => {
                    if (typeof model.command === 'function') {
                        let callback = model.command ;
                        model.command = () => {
                            this.confirmationService.confirm({
                                accept: () => {
                                    callback();
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
        if (this.selected) {
            setTimeout(() => {
                this.dynamicOverflow = 'visible';
            }, 300);
        }
    }
}

export interface ButtonAction {
    label: string;
    model: ButtonModel[];
}

export interface ButtonModel {
    deleteOptions?: any;
    icon: string;
    command: any;
    label: string;
    isDelete?: boolean;
}