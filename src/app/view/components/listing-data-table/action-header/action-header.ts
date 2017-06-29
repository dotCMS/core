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
    @Input() selectedItems = [];
    @Input() options: ActionHeaderOptions;
    public dynamicOverflow = 'visible';

    constructor(messageService: MessageService, private confirmationService: ConfirmationService) {
        super(['selected'], messageService);
    }

    private ngOnChanges(changes: SimpleChanges): any {
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
                        let callback = model.command ;
                        model.command = ($event) => {
                            let originalEvent = $event;
                            this.confirmationService.confirm({
                                accept: ($event) => {
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

export interface ButtonAction {
    label: string;
    model: ButtonModel[];
}

export interface ButtonModel {
    command: any;
    deleteOptions?: ActionHeaderDeleteOptions;
    icon?: string;
    isDelete?: boolean;
    label: string;
}

export interface ActionHeaderOptions {
    primary?: ActionHeaderOptionsPrimary;
    secondary?: ButtonAction[];
}

export interface ActionHeaderDeleteOptions {
    confirmHeader?: string;
    confirmMessage?: string;
}

export interface ActionHeaderOptionsPrimary {
    command: (event?: any) => void;
    model: ButtonModel[];
}