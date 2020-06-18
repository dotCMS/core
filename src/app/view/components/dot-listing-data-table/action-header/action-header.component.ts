import { DotAlertConfirmService } from '@services/dot-alert-confirm/dot-alert-confirm.service';
import { Component, Input, SimpleChanges, ViewEncapsulation, OnChanges } from '@angular/core';

import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { ActionHeaderOptions, ButtonAction } from '@models/action-header';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'dot-action-header',
    styleUrls: ['./action-header.component.scss'],
    templateUrl: 'action-header.component.html'
})
export class ActionHeaderComponent implements OnChanges {
    @Input() selectedItems = [];

    @Input() options: ActionHeaderOptions;

    public dynamicOverflow = 'visible';

    constructor(
        private dotMessageService: DotMessageService,
        private dotDialogService: DotAlertConfirmService
    ) {}

    ngOnChanges(changes: SimpleChanges): any {
        if (changes.selected) {
            this.hideDinamycOverflow();
        }

        if (this.hasSecondary(changes)) {
            this.setCommandWrapper(changes.options.currentValue.secondary);
        }
    }

    /**
     * Trigger button primary actions if is defined
     *
     * @memberof ActionHeaderComponent
     */
    handlePrimaryAction(): void {
        if (this.options.primary.command) {
            this.options.primary.command();
        }
    }

    private setCommandWrapper(options: ButtonAction[]): void {
        options.forEach(actionButton => {
            actionButton.model.filter(model => model.deleteOptions).forEach(model => {
                if (typeof model.command === 'function') {
                    const callback = model.command;
                    model.command = $event => {
                        const originalEvent = $event;

                        this.dotDialogService.confirm({
                            accept: () => {
                                callback(originalEvent);
                            },
                            header: model.deleteOptions.confirmHeader,
                            message: model.deleteOptions.confirmMessage,
                            footerLabel: {
                                accept: this.dotMessageService.get('contenttypes.action.delete'),
                                reject: this.dotMessageService.get('contenttypes.action.cancel')
                            }
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

    private hasSecondary(changes: SimpleChanges): boolean {
        return (
            changes.options &&
            changes.options.currentValue &&
            changes.options.currentValue.secondary
        );
    }
}
