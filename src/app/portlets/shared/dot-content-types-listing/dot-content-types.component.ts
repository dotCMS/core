import { forkJoin } from 'rxjs';
import * as _ from 'lodash';

import { map, take, pluck } from 'rxjs/operators';
import { DotListingDataTableComponent } from '@components/dot-listing-data-table/dot-listing-data-table.component';
import { DotAlertConfirmService } from '@services/dot-alert-confirm/dot-alert-confirm.service';
import { DotCrudService } from '@services/dot-crud';
import { ActivatedRoute, Router } from '@angular/router';
import { Component, OnInit, ViewChild } from '@angular/core';

import { ActionHeaderOptions } from '@models/action-header';
import { DotContentTypesInfoService } from '@services/dot-content-types-info';
import { DataTableColumn } from '@models/data-table';
import { DotMessageService } from '@services/dot-messages-service';
import { StructureTypeView } from '@models/contentlet/structure-type-view.model';
import { ButtonModel } from '@models/action-header/button.model';
import { DotDataTableAction } from '@models/data-table/dot-data-table-action';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import { DotEnvironment } from '@models/dot-environment/dot-environment';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotContentTypeService } from '@services/dot-content-type/dot-content-type.service';
import { DotPushPublishDialogService } from 'dotcms-js';

/**
 * List of Content Types
 * use: listing-data-table.component
 * @export
 * @class DotContentTypesPortletComponent

 */
@Component({
    selector: 'dot-content-types',
    styleUrls: ['./dot-content-types.component.scss'],
    templateUrl: 'dot-content-types.component.html'
})
export class DotContentTypesPortletComponent implements OnInit {
    @ViewChild('listing') listing: DotListingDataTableComponent;
    filterBy: string;
    public contentTypeColumns: DataTableColumn[];
    public item: any;
    public actionHeaderOptions: ActionHeaderOptions;
    public rowActions: DotDataTableAction[];
    public addToBundleIdentifier: string;

    constructor(
        private contentTypesInfoService: DotContentTypesInfoService,
        private crudService: DotCrudService,
        private dotContentTypeService: DotContentTypeService,
        private dotDialogService: DotAlertConfirmService,
        private dotLicenseService: DotLicenseService,
        private httpErrorManagerService: DotHttpErrorManagerService,
        private pushPublishService: PushPublishService,
        private route: ActivatedRoute,
        private router: Router,
        private dotMessageService: DotMessageService,
        private dotPushPublishDialogService: DotPushPublishDialogService
    ) {}

    ngOnInit() {
        forkJoin(
            this.dotContentTypeService.getAllContentTypes(),
            this.dotLicenseService.isEnterprise(),
            this.pushPublishService
                .getEnvironments()
                .pipe(map((environments: DotEnvironment[]) => !!environments.length), take(1)),
            this.route.data.pipe(pluck('filterBy'), take(1))
        ).subscribe(([ contentTypes, isEnterprise, environments, filterBy]) => {
            const baseTypes: StructureTypeView[] = contentTypes;
            const rowActionsMap = {
                pushPublish: isEnterprise && environments,
                addToBundle: isEnterprise
            };

            this.actionHeaderOptions = {
                primary: {
                    model: this.setContentTypes(baseTypes)
                }
            };

            this.contentTypeColumns = this.setContentTypeColumns();
            this.rowActions = this.createRowActions(rowActionsMap);
            if (filterBy) {
                this.setFilterByContentType(filterBy as string);
            }
        });
    }

    editContentType($event): void {
        this.router.navigate([`edit/${$event.data.id}`], {
            relativeTo: this.route
        });
    }

    public changeBaseTypeSelector(value: string) {
        value !== ''
            ? this.listing.paginatorService.setExtraParams('type', value)
            : this.listing.paginatorService.deleteExtraParams('type');
        this.listing.loadFirstPage();
    }

    private setFilterByContentType(contentType: string) {
        this.filterBy = _.startCase(_.toLower(contentType));
        this.listing.paginatorService.setExtraParams('type', this.filterBy);

        this.actionHeaderOptions.primary.command = $event => {
            this.createContentType(null, $event);
        };
        this.actionHeaderOptions.primary.model = null;
    }

    private getPublishActions(pushPublish: boolean, addToBundle: boolean): DotDataTableAction[] {
        const actions: DotDataTableAction[] = [];
        /*
            Only show Push Publish action if DotCMS instance have the appropriate license and there are
            push publish environments created.
        */
        if (pushPublish) {
            actions.push({
                menuItem: {
                    label: this.dotMessageService.get('contenttypes.content.push_publish'),
                    command: item => this.pushPublishContentType(item)
                }
            });
        }

        if (addToBundle) {
            actions.push({
                menuItem: {
                    label: this.dotMessageService.get('contenttypes.content.add_to_bundle'),
                    command: item => this.addToBundleContentType(item)
                }
            });
        }

        return actions;
    }

    private createRowActions(rowActionsMap: any): DotDataTableAction[] {
        const listingActions: DotDataTableAction[] = [
            ...this.getPublishActions(rowActionsMap.pushPublish, rowActionsMap.addToBundle)
        ];

        listingActions.push({
            menuItem: {
                label: this.dotMessageService.get('contenttypes.action.delete'),
                command: item => this.removeConfirmation(item),
                icon: 'delete'
            },
            shouldShow: item => !item.fixed && !item.defaultType
        });

        /*
            If we have more than one action it means that we'll show the contextual menu and we don't want icons there
        */
        return listingActions.length > 1
            ? listingActions.map(this.removeIconsFromMenuItem)
            : listingActions;
    }

    private removeIconsFromMenuItem(action: DotDataTableAction): DotDataTableAction {
        const { icon, ...noIconMenuItem } = action.menuItem;
        return {
            ...action,
            menuItem: noIconMenuItem
        };
    }

    private setContentTypes(s: StructureTypeView[]): ButtonModel[] {
        return s.map((structureTypeView: StructureTypeView) => {
            return {
                command: $event => {
                    this.createContentType(structureTypeView.name.toLocaleLowerCase(), $event);
                },
                icon: this.contentTypesInfoService.getIcon(`${structureTypeView.name}_old`),
                label: this.dotMessageService.get(
                    'contenttypes.content.' + structureTypeView.name.toLocaleLowerCase()
                )
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
                width: '8%',
                textAlign: 'left',
                textContent: this.dotMessageService.get('dot.common.view')
            },
            {
                fieldName: 'modDate',
                format: 'date',
                header: this.dotMessageService.get('contenttypes.fieldname.last_edit_date'),
                sortable: true,
                width: '13%'
            }
        ];
    }

    private createContentType(type: string, _event?): void {
        const params = ['create'];
        if (type) {
            params.push(type);
        }
        this.router.navigate(params, { relativeTo: this.route });
    }

    private removeConfirmation(item: any): void {
        this.dotDialogService.confirm({
            accept: () => {
                this.removeContentType(item);
            },
            header: this.dotMessageService.get('message.structure.cantdelete'),
            message: this.dotMessageService.get(
                'contenttypes.confirm.message.delete.type',
                item.name
            ),
            footerLabel: {
                accept: this.dotMessageService.get('contenttypes.action.delete'),
                reject: this.dotMessageService.get('contenttypes.action.cancel')
            }
        });
    }

    private removeContentType(item): void {
        this.crudService
            .delete(`v1/contenttype/id`, item.id)
            .pipe(take(1))
            .subscribe(
                () => {
                    this.listing.loadCurrentPage();
                },
                error =>
                    this.httpErrorManagerService
                        .handle(error)
                        .pipe(take(1))
                        .subscribe()
            );
    }

    private pushPublishContentType(item: any) {
        this.dotPushPublishDialogService.open({
            assetIdentifier: item.id,
            title: this.dotMessageService.get('contenttypes.content.push_publish')
        });
    }

    private addToBundleContentType(item: any) {
        this.addToBundleIdentifier = item.id;
    }
}
