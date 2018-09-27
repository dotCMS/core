import { DotAlertConfirmService } from '@services/dot-alert-confirm/dot-alert-confirm.service';
import {
    Component,
    Input,
    SimpleChanges,
    ViewEncapsulation,
    OnChanges,
    OnInit
} from '@angular/core';

import { DotMessageService } from '@services/dot-messages-service';
import { ActionHeaderOptions, ButtonAction } from '@models/action-header';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'dot-action-header',
    styleUrls: ['./action-header.scss'],
    templateUrl: 'action-header.html'
})
export class ActionHeaderComponent implements OnChanges, OnInit {
    @Input()
    selectedItems = [];

    @Input()
    options: ActionHeaderOptions;

    public dynamicOverflow = 'visible';

    i18nMessages: {
        [key: string]: string;
    } = {};

    constructor(
        private dotMessageService: DotMessageService,
        private dotDialogService: DotAlertConfirmService
    ) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages(['selected', 'contenttypes.action.delete', 'contenttypes.action.cancel'])
            .subscribe((res) => {
                this.i18nMessages = res;
            });
    }

    ngOnChanges(changes: SimpleChanges): any {
        if (changes.selected) {
            this.hideDinamycOverflow();
        }

        if (
            changes.options &&
            changes.options.currentValue &&
            changes.options.currentValue.secondary
        ) {
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
        options.forEach((actionButton) => {
            actionButton.model.filter((model) => model.deleteOptions).forEach((model) => {
                if (typeof model.command === 'function') {
                    const callback = model.command;
                    model.command = ($event) => {
                        const originalEvent = $event;

                        this.dotDialogService.confirm({
                            accept: () => {
                                callback(originalEvent);
                            },
                            header: model.deleteOptions.confirmHeader,
                            message: model.deleteOptions.confirmMessage,
                            footerLabel: {
                                accept: this.i18nMessages['contenttypes.action.delete'],
                                reject: this.i18nMessages['contenttypes.action.cancel']
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
