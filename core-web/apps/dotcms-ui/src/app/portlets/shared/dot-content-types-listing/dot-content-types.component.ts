import { forkJoin, Subject } from 'rxjs';

import { Component, OnDestroy, OnInit, ViewChild, ViewContainerRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { map, pluck, take, takeUntil } from 'rxjs/operators';

import { DotListingDataTableComponent } from '@components/dot-listing-data-table/dot-listing-data-table.component';
import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotContentTypesInfoService,
    DotCrudService,
    DotHttpErrorManagerService,
    DotLicenseService,
    DotMessageService,
    PushPublishService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import {
    DotActionMenuItem,
    DotCMSBaseTypesContentTypes,
    DotCMSContentType,
    DotCopyContentTypeDialogFormFields,
    DotEnvironment,
    StructureTypeView
} from '@dotcms/dotcms-models';
import { ActionHeaderOptions } from '@models/action-header';
import { ButtonModel } from '@models/action-header/button.model';
import { DataTableColumn } from '@models/data-table';

import { DotContentTypeStore } from './dot-content-type.store';

type DotRowActions = {
    pushPublish: boolean;
    addToBundle: boolean;
    addToMenu: boolean;
    cloneContentType: boolean;
};

/**
 * List of Content Types
 *
 * @export
 * @class DotContentTypesPortletComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-content-types',
    styleUrls: ['./dot-content-types.component.scss'],
    templateUrl: 'dot-content-types.component.html',
    providers: [DotContentTypeStore]
})
export class DotContentTypesPortletComponent implements OnInit, OnDestroy {
    @ViewChild('listing', { static: false })
    listing: DotListingDataTableComponent;
    filterBy: string;
    showTable = false;
    paginatorExtraParams: { [key: string]: string };
    contentTypeColumns: DataTableColumn[];
    actionHeaderOptions: ActionHeaderOptions;
    rowActions: DotActionMenuItem[];
    addToBundleIdentifier: string;
    addToMenuContentType: DotCMSContentType;

    @ViewChild('dotDynamicDialog', { read: ViewContainerRef, static: true })
    public dotDynamicDialog: ViewContainerRef;
    private destroy$: Subject<boolean> = new Subject<boolean>();
    private dialogDestroy$: Subject<boolean> = new Subject<boolean>();

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
        private dotPushPublishDialogService: DotPushPublishDialogService,
        private dotContentTypeStore: DotContentTypeStore
    ) {}

    ngOnInit() {
        forkJoin(
            this.dotContentTypeService.getAllContentTypes(),
            this.dotLicenseService.isEnterprise(),
            this.pushPublishService.getEnvironments().pipe(
                map((environments: DotEnvironment[]) => !!environments.length),
                take(1)
            ),
            this.route.data.pipe(pluck('filterBy'), take(1))
        ).subscribe(([contentTypes, isEnterprise, environments, filterBy]) => {
            const baseTypes: StructureTypeView[] = contentTypes;

            this.actionHeaderOptions = {
                primary: {
                    model: this.setContentTypes(baseTypes)
                }
            };

            this.contentTypeColumns = this.setContentTypeColumns();
            this.rowActions = this.createRowActions({
                pushPublish: !!(isEnterprise && environments),
                addToBundle: isEnterprise,
                addToMenu: isEnterprise,
                cloneContentType: isEnterprise
            });

            if (filterBy) {
                this.setFilterByContentType(filterBy as string);
            }

            this.showTable = true;
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Handler to edit a content type
     *
     * @param {DotCMSContentType} item
     * @memberof DotContentTypesPortletComponent
     */
    editContentType(item: DotCMSContentType): void {
        this.router.navigate([`edit/${item.id}`], {
            relativeTo: this.route
        });
    }

    /**
     * Change base type to the selected one
     *
     * @param {string} value
     * @memberof DotContentTypesPortletComponent
     */
    changeBaseTypeSelector(value: string) {
        value !== ''
            ? this.listing.paginatorService.setExtraParams('type', value)
            : this.listing.paginatorService.deleteExtraParams('type');
        this.listing.loadFirstPage();
    }

    /**
     * Save the form values of Copy Dialog
     *
     * @param {DotCopyContentTypeDialogFormFields} form
     * @memberof DotContentTypesPortletComponent
     */
    saveCloneContentTypeDialog(form: DotCopyContentTypeDialogFormFields) {
        this.dotContentTypeStore.saveCopyDialog(form);
    }

    private setFilterByContentType(contentType: string) {
        const lowerCased = contentType.toLowerCase();

        this.filterBy = lowerCased.charAt(0).toUpperCase() + lowerCased.slice(1);

        this.paginatorExtraParams = { type: this.filterBy };
        this.actionHeaderOptions.primary.command = ($event) => {
            this.createContentType(null, $event);
        };

        this.actionHeaderOptions.primary.model = null;
    }

    private createRowActions(rowActionsMap: DotRowActions): DotActionMenuItem[] {
        const deleteAction: DotActionMenuItem[] = [
            {
                menuItem: {
                    label: this.dotMessageService.get('contenttypes.action.delete'),
                    command: (item: DotCMSContentType) => this.removeConfirmation(item),
                    icon: 'pi pi-trash'
                },
                shouldShow: (item) => !item.fixed && !item.defaultType
            }
        ];

        const listingActions: DotActionMenuItem[] = [
            ...this.getPublishActions(rowActionsMap),
            ...deleteAction
        ];

        /*
            If we have more than one action it means that we'll show the contextual menu and we don't want icons there
        */
        return listingActions.length > 1
            ? listingActions.map(this.removeIconsFromMenuItem)
            : listingActions;
    }

    private getPublishActions({
        pushPublish,
        addToBundle,
        addToMenu,
        cloneContentType
    }: DotRowActions) {
        const actions: DotActionMenuItem[] = [];
        /*
            Only show Push Publish action if DotCMS instance have the appropriate license and there are
            push publish environments created.
        */
        if (pushPublish) {
            actions.push({
                menuItem: {
                    label: this.dotMessageService.get('contenttypes.content.push_publish'),
                    command: (item: DotCMSContentType) => this.pushPublishContentType(item)
                }
            });
        }

        if (addToBundle) {
            actions.push({
                menuItem: {
                    label: this.dotMessageService.get('contenttypes.content.add_to_bundle'),
                    command: (item: DotCMSContentType) => this.addToBundleContentType(item)
                }
            });
        }

        if (addToMenu) {
            actions.push({
                menuItem: {
                    label: this.dotMessageService.get('contenttypes.content.add_to_menu'),
                    command: (item: DotCMSContentType) => this.addToBundleMenu(item)
                },
                shouldShow: (item: Record<string, unknown>) => {
                    return item.variable !== 'Host';
                }
            });
        }

        if (cloneContentType) {
            actions.push({
                menuItem: {
                    label: this.dotMessageService.get('contenttypes.content.copy'),
                    command: (item: DotCMSContentType) => {
                        this.showCloneContentTypeDialog(item);
                    }
                }
            });
        }

        return actions;
    }

    private removeIconsFromMenuItem(action: DotActionMenuItem): DotActionMenuItem {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const { icon, ...noIconMenuItem } = action.menuItem;

        return {
            ...action,
            menuItem: noIconMenuItem
        };
    }

    private setContentTypes(s: StructureTypeView[]): ButtonModel[] {
        return s.map((structureTypeView: StructureTypeView) => {
            return {
                command: ($event) => {
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
                icon: (item: { icon: string }): string => item.icon,
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

    private removeConfirmation(item: DotCMSContentType): void {
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

    private removeContentType(item: DotCMSContentType): void {
        this.crudService
            .delete(`v1/contenttype/id`, item.id)
            .pipe(take(1))
            .subscribe(
                () => {
                    this.listing.loadCurrentPage();
                },
                (error) => this.httpErrorManagerService.handle(error).pipe(take(1)).subscribe()
            );
    }

    private pushPublishContentType(item: DotCMSContentType) {
        this.dotPushPublishDialogService.open({
            assetIdentifier: item.id,
            title: this.dotMessageService.get('contenttypes.content.push_publish')
        });
    }

    private async showCloneContentTypeDialog(item: DotCMSContentType) {
        const { DotContentTypeCopyDialogComponent } = await import(
            './components/dot-content-type-copy-dialog/dot-content-type-copy-dialog.component'
        );
        const componentRef = this.dotDynamicDialog.createComponent(
            DotContentTypeCopyDialogComponent
        );

        this.dotContentTypeStore.setAssetSelected(item.id);

        componentRef.instance.openDialog({
            assetIdentifier: item.id,
            title: `${this.dotMessageService.get('contenttypes.content.copy')} ${item.name}`,
            baseType: item.baseType as DotCMSBaseTypesContentTypes,
            data: {
                icon: item.icon,
                host: item.host
            }
        });

        componentRef.instance.isSaving$ = this.dotContentTypeStore.isSaving$;
        componentRef.instance.cancelBtn.pipe(takeUntil(this.dialogDestroy$)).subscribe(() => {
            this.closeCopyContentTypeDialog();
        });
        componentRef.instance.validFormFields
            .pipe(takeUntil(this.dialogDestroy$))
            .subscribe((formValues) => {
                this.saveCloneContentTypeDialog(formValues);
            });
    }

    private addToBundleContentType(item: DotCMSContentType) {
        this.addToBundleIdentifier = item.id;
    }

    private addToBundleMenu(item: DotCMSContentType) {
        this.addToMenuContentType = item;
    }

    private closeCopyContentTypeDialog() {
        this.dialogDestroy$.next(true);
        this.dialogDestroy$.complete();
        this.dotDynamicDialog.clear();
    }
}
