import { ContentType } from './../shared/content-type.model';
import { ListingDataTableComponent } from './../../../view/components/listing-data-table/listing-data-table.component';
import { DotConfirmationService } from './../../../api/services/dot-confirmation/dot-confirmation.service';
import { CrudService } from './../../../api/services/crud';
import { MenuItem } from 'primeng/primeng';
import { ActivatedRoute, Router } from '@angular/router';
import { Component, ViewChild } from '@angular/core';

import { ActionHeaderOptions } from '../../../shared/models/action-header';
import { BaseComponent } from '../../../view/components/_common/_base/base-component';
import { ContentTypesInfoService } from '../../../api/services/content-types-info';
import { DataTableColumn } from '../../../shared/models/data-table';
import { MessageService } from '../../../api/services/messages-service';

/**
 * List of Content Types
 * use: listing-data-table.component
 * @export
 * @class ContentTypesPortletComponent
 * @extends {BaseComponent}
 */
@Component({
    selector: 'content-types',
    styleUrls: ['./content-types.component.scss'],
    templateUrl: 'content-types.component.html'
})
export class ContentTypesPortletComponent extends BaseComponent {
    public contentTypeColumns: DataTableColumn[];
    public item: any;
    public actionHeaderOptions: ActionHeaderOptions;
    public rowActions: MenuItem[];
    @ViewChild('listing') listing: ListingDataTableComponent;

    constructor(
        messageService: MessageService,
        private router: Router,
        private route: ActivatedRoute,
        private contentTypesInfoService: ContentTypesInfoService,
        private crudService: CrudService,
        private dotConfirmationService: DotConfirmationService
    ) {
        super(
            [
                'contenttypes.fieldname.structure.name',
                'contenttypes.content.variable',
                'contenttypes.form.label.description',
                'contenttypes.fieldname.entries',
                'message.structure.delete.structure.and.content',
                'message.structure.cantdelete',
                'contenttypes.content.file',
                'contenttypes.content.content',
                'contenttypes.content.persona',
                'contenttypes.content.widget',
                'contenttypes.content.page',
                'contenttypes.confirm.message.delete',
                'contenttypes.confirm.message.delete.content',
                'contenttypes.confirm.message.delete.warning',
                'contenttypes.action.delete',
                'contenttypes.action.cancel',
                'Content-Type'
            ],
            messageService
        );
    }

    /**
     * Callback call from BaseComponent when the messages are received.
     * @memberOf ContentTypesPortletComponent
     */
    onMessage(): void {
        this.actionHeaderOptions = {
            primary: {
                command: $event => {
                    this.createContentType($event);
                },
                model: [
                    {
                        command: $event => {
                            this.createContentType('content', $event);
                        },
                        icon: 'fa-newspaper-o',
                        label: this.i18nMessages['contenttypes.content.content']
                    },
                    {
                        command: $event => {
                            this.createContentType('widget', $event);
                        },
                        icon: 'fa-cog',
                        label: this.i18nMessages['contenttypes.content.widget']
                    },
                    {
                        command: $event => {
                            this.createContentType('file', $event);
                        },
                        icon: 'fa-file-o',
                        label: this.i18nMessages['contenttypes.content.file']
                    },
                    {
                        command: $event => {
                            this.createContentType('page', $event);
                        },
                        icon: 'fa-file-text-o',
                        label: this.i18nMessages['contenttypes.content.page']
                    },
                    {
                        command: $event => {
                            this.createContentType('persona', $event);
                        },
                        icon: 'fa-user',
                        label: this.i18nMessages['contenttypes.content.persona']
                    }
                ]
            }
        };

        this.contentTypeColumns = [
            {
                fieldName: 'name',
                header: this.i18nMessages['contenttypes.fieldname.structure.name'],
                icon: (item: any): string => this.contentTypesInfoService.getIcon(item.baseType),
                sortable: true
            },
            {
                fieldName: 'variable',
                header: this.i18nMessages['contenttypes.content.variable'],
                sortable: true
            },
            {
                fieldName: 'description',
                header: this.i18nMessages['contenttypes.form.label.description'],
                sortable: true
            },
            {
                fieldName: 'nEntries',
                header: this.i18nMessages['contenttypes.fieldname.entries'],
                width: '7%'
            },
            {
                fieldName: 'modDate',
                format: 'date',
                header: 'Last Edit Date',
                sortable: true,
                width: '13%'
            }
        ];

        this.rowActions = [
            {
                label: 'Remove',
                icon: 'fa-trash',
                command: (item) => this.removeConfirmation(item)
            }
        ];
    }

    private createContentType(type: string, $event?): void {
        this.router.navigate(['create', type], { relativeTo: this.route });
    }

    private editContentType($event): void {
        this.router.navigate([`edit/${$event.data.id}`], {
            relativeTo: this.route
        });
    }

    private removeConfirmation(item: any): void {
        this.dotConfirmationService.confirm({
            accept: () => {
                this.removeContentType(item);
            },
            header: this.i18nMessages['message.structure.cantdelete'],
            message: `${this.i18nMessages['contenttypes.confirm.message.delete']} ${this.i18nMessages['Content-Type']}
                        ${this.i18nMessages['contenttypes.confirm.message.delete.content']}
                        <span>${this.i18nMessages['contenttypes.confirm.message.delete.warning']}</span>`,
            footerLabel: {
                acceptLabel: this.i18nMessages['contenttypes.action.delete'],
                rejectLabel: this.i18nMessages['contenttypes.action.cancel']
            }
        });
    }

    private removeContentType(item): void {
        this.crudService.delete(`v1/contenttype/id`, item.id).subscribe(data => {
            this.listing.loadCurrentPage();
        });
    }
}
