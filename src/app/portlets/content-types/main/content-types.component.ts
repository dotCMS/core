import { ListingDataTableComponent } from './../../../view/components/listing-data-table/listing-data-table.component';
import { DotConfirmationService } from './../../../api/services/dot-confirmation/dot-confirmation.service';
import { CrudService } from './../../../api/services/crud';
import { MenuItem } from 'primeng/primeng';
import { ActivatedRoute, Router } from '@angular/router';
import { Component, OnInit, ViewChild } from '@angular/core';

import { ActionHeaderOptions } from '../../../shared/models/action-header';
import { ContentTypesInfoService } from '../../../api/services/content-types-info';
import { DataTableColumn } from '../../../shared/models/data-table';
import { DotMessageService } from '../../../api/services/dot-messages-service';
import { Observable } from 'rxjs/Observable';
import { DotContentletService } from '../../../api/services/dot-contentlet.service';
import { StructureTypeView } from '../../../shared/models/contentlet/structure-type-view.model';
import { ButtonModel } from '../../../shared/models/action-header/button.model';
import { DotDataTableAction } from '../../../shared/models/data-table/dot-data-table-action';
import { DotcmsConfig, ConfigParams } from 'dotcms-js/dotcms-js';
import { PushPublishService } from '../../../api/services/push-publish/push-publish.service';
import { DotEnvironment } from '../../../shared/models/dot-environment/dot-environment';

/**
 * List of Content Types
 * use: listing-data-table.component
 * @export
 * @class ContentTypesPortletComponent

 */
@Component({
    selector: 'dot-content-types',
    styleUrls: ['./content-types.component.scss'],
    templateUrl: 'content-types.component.html'
})
export class ContentTypesPortletComponent implements OnInit {
    @ViewChild('listing') listing: ListingDataTableComponent;
    public contentTypeColumns: DataTableColumn[];
    public item: any;
    public actionHeaderOptions: ActionHeaderOptions;
    public rowActions: DotDataTableAction[];
    public pushPublishIdentifier: string;

    private i18nKeys = [
        'contenttypes.fieldname.structure.name',
        'contenttypes.content.variable',
        'contenttypes.form.label.description',
        'contenttypes.fieldname.entries',
        'message.structure.delete.structure.and.content',
        'message.structure.cantdelete',
        'contenttypes.content.fileasset',
        'contenttypes.content.content',
        'contenttypes.content.persona',
        'contenttypes.content.widget',
        'contenttypes.content.htmlpage',
        'contenttypes.content.key_value',
        'contenttypes.content.vanity_url',
        'contenttypes.content.form',
        'contenttypes.confirm.message.delete',
        'contenttypes.confirm.message.delete.content',
        'contenttypes.confirm.message.delete.warning',
        'contenttypes.action.delete',
        'contenttypes.action.cancel',
        'contenttypes.content.push_publish',
        'Content-Type'
    ];

    constructor(
        private contentTypesInfoService: ContentTypesInfoService,
        private crudService: CrudService,
        private dotConfirmationService: DotConfirmationService,
        private dotContentletService: DotContentletService,
        private dotcmsConfig: DotcmsConfig,
        private route: ActivatedRoute,
        private router: Router,
        public dotMessageService: DotMessageService,
        private pushPublishService: PushPublishService
    ) { }

    ngOnInit() {
        Observable.forkJoin(
            this.dotMessageService.getMessages(this.i18nKeys),
            this.dotContentletService.getAllContentTypes(),
            this.dotcmsConfig.getConfig().take(1).map((config: ConfigParams) => {
                /*
                    TODO: need to update ConfigParams interface and create a License interface
                */
                const license: any = config.license;
                return license.isCommunity;
            }),
            this.pushPublishService.getEnvironments().map((environments: DotEnvironment[]) => !!environments.length)
        ).subscribe(res => {
            const baseTypes: StructureTypeView[] = res[1];
            const rowActionsMap = {
                delete: true,
                pushPublish: !res[2] && res[3]
            };

            console.log(res[3]);

            this.actionHeaderOptions = {
                primary: {
                    command: $event => {
                        this.createContentType($event);
                    },
                    model: this.setContentTypes(baseTypes)
                }
            };

            this.contentTypeColumns = this.setContentTypeColumns();
            this.rowActions = this.createRowActions(rowActionsMap);
        });
    }

    private createRowActions(rowActionsMap: any): DotDataTableAction[] {
        const listingActions: DotDataTableAction[] = [];

        if (rowActionsMap.delete) {
            listingActions.push({
                menuItem: {
                    label: this.dotMessageService.get('contenttypes.action.delete'),
                    command: item => this.removeConfirmation(item),
                    icon: 'fa-trash'
                },
                shouldShow: item => !item.fixed
            });
        }


        /*
            Only show Push Publish action if DotCMS instance have the appropriate license and there are
            push publish environments created.
        */
        if (rowActionsMap.pushPublish) {
            listingActions.push({
                menuItem: {
                    label: this.dotMessageService.get('contenttypes.content.push_publish'),
                    command: item => this.pushPublishContentType(item)
                }
            });
        }

        /*
            If we have more than one action it means that we'll show the contextual menu and we don't want icons there
        */
        return listingActions.length > 1 ? listingActions.map(this.removeIconsFromMenuItem) : listingActions;
    }

    private removeIconsFromMenuItem(action: DotDataTableAction): DotDataTableAction {
        const { icon, ...noIconMenuItem} = action.menuItem;
        return {
            ... action,
            menuItem: noIconMenuItem
        };
    }

    private setContentTypes(s: StructureTypeView[]): ButtonModel[] {
        return s.map(structureTypeView => {
            return {
                command: $event => {
                    this.createContentType(structureTypeView.name.toLocaleLowerCase(), $event);
                },
                icon: this.contentTypesInfoService.getIcon(structureTypeView.name),
                label: this.dotMessageService.get('contenttypes.content.' + structureTypeView.name.toLocaleLowerCase())
            };
        });
    }

    private setContentTypeColumns(): DataTableColumn[] {
        return [
            {
                fieldName: 'name',
                header: this.dotMessageService.get('contenttypes.fieldname.structure.name'),
                icon: (item: any): string => this.contentTypesInfoService.getIcon(item.baseType),
                sortable: true
            },
            {
                fieldName: 'variable',
                header: this.dotMessageService.get('contenttypes.content.variable'),
                sortable: true
            },
            {
                fieldName: 'description',
                header: this.dotMessageService.get('contenttypes.form.label.description'),
                sortable: true
            },
            {
                fieldName: 'nEntries',
                header: this.dotMessageService.get('contenttypes.fieldname.entries'),
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
            header: this.dotMessageService.get('message.structure.cantdelete'),
            message: `${this.dotMessageService.get('contenttypes.confirm.message.delete')} ${this.dotMessageService.get(
                'Content-Type'
            )}
                        ${this.dotMessageService.get('contenttypes.confirm.message.delete.content')}
                        <span>${this.dotMessageService.get('contenttypes.confirm.message.delete.warning')}</span>`,
            footerLabel: {
                acceptLabel: this.dotMessageService.get('contenttypes.action.delete'),
                rejectLabel: this.dotMessageService.get('contenttypes.action.cancel')
            }
        });
    }

    private removeContentType(item): void {
        this.crudService.delete(`v1/contenttype/id`, item.id).subscribe(data => {
            this.listing.loadCurrentPage();
        });
    }

    private closeDialog(): void {
        this.pushPublishIdentifier = null;
    }

    private pushPublishContentType(item: any) {
        this.pushPublishIdentifier = item.id;
    }
}
