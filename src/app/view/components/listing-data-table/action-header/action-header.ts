import { DotConfirmationService } from './../../../../api/services/dot-confirmation/dot-confirmation.service';
import { Component, Input, SimpleChanges, ViewEncapsulation, OnChanges } from '@angular/core';

import { BaseComponent } from '../../_common/_base/base-component';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
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

    constructor(dotMessageService: DotMessageService, private dotConfirmationService: DotConfirmationService) {
        super(
            [
                'selected',
                'contenttypes.action.delete',
                'contenttypes.action.cancel'
            ], dotMessageService
        );
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

                            this.dotConfirmationService.confirm({
                                accept: () => {
                                    callback(originalEvent);
                                },
                                header: model.deleteOptions.confirmHeader,
                                message: model.deleteOptions.confirmMessage,
                                footerLabel: {
                                    acceptLabel: this.i18nMessages['contenttypes.action.delete'],
                                    rejectLabel: this.i18nMessages['contenttypes.action.cancel']
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
}
