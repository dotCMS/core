import { BaseComponent } from '../../_common/_base/base-component';
import { Component, Input, SimpleChanges, ViewEncapsulation } from '@angular/core';
import { ConfirmationService } from 'primeng/primeng';
import { MessageService } from '../../../../api/services/messages-service';
import { LoggerService } from '../../../../api/services/logger.service';
import { Router } from '@angular/router';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'action-header',
    styles: [require('./action-header.scss')],
    templateUrl: 'action-header.html'
})

export class ActionHeaderComponent extends BaseComponent {
    @Input() selected = false;
    @Input() selectedItems = [];
    @Input() actionButtonItems: ButtonAction[];

    public dynamicOverflow = 'visible';

    private contentTypeActions;

    constructor( messageService: MessageService, public loggerService: LoggerService, private confirmationService: ConfirmationService,
                    private router: Router) {

        super(['selected'], messageService);

        this.contentTypeActions = [{
                command: () => {
                    this.createContentType('content');
                },
                icon: 'fa-newspaper-o',
                label: 'Content'
            },
            {
                command: () => {
                    this.createContentType('widget');
                },
                icon: 'fa-cog',
                label: 'Widget'
            },
            {
                command: () => {
                    this.createContentType('file');
                },
                icon: 'fa-file-o',
                label: 'File'
            },
            {
                command: () => {
                    this.createContentType('page');
                },
                icon: 'fa-file-text-o',
                label: 'Page'
            },
            {
                command: () => {
                    this.createContentType('persona');
                },
                icon: 'fa-user',
                label: 'Persona'
            }];
    }

    private createContentType(type): void {
        this.router.navigate(['/content-types-angular/create', type]);
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